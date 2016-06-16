package uk.ac.ebi.biosamples.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class XMLUtils {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    private TransformerFactory tf = TransformerFactory.newInstance();
    
	private CloseableHttpClient httpClient;  
	
	@PostConstruct
	public void setupConnectionManager() {
    	PoolingHttpClientConnectionManager conman = new PoolingHttpClientConnectionManager();
    	conman.setMaxTotal(128);
    	conman.setDefaultMaxPerRoute(64);
    	conman.setValidateAfterInactivity(0);
    	
    	ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            	//see if the user provides a live time
                HeaderElementIterator it = new BasicHeaderElementIterator
                    (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase
                       ("timeout")) {
                        return Long.parseLong(value) * 1000;
                    }
                }
                //default to one second live time 
                return 1 * 1000;
            }
        };
    	
    	httpClient = HttpClients.custom()
    			.setKeepAliveStrategy(keepAliveStrategy)
    			.setConnectionManager(conman).build();
    }
    
    static {
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreWhitespace(true);
    }
    
    public Document getDocument(File xmlFile) throws FileNotFoundException, DocumentException {
        return getDocument(new BufferedReader(new FileReader(xmlFile)));
    }

    public  Document getDocument(URL url) throws DocumentException, IOException {  
        //can't call SAXReader directly because it ignores proxing
        Document doc = null;
        HttpGet get = new HttpGet(url.toString());
        try (CloseableHttpResponse response = httpClient.execute(get)) {
	        try (Reader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
	            doc = getDocument(r);
	        }         
        }
        return doc;
    }

    public Document getDocument(String xmlString) throws DocumentException {
        
        Reader r = null;
        Document doc = null;
        try {
            r = new StringReader(xmlString);
            doc = getDocument(r);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
        return doc;
    }
    
    public Document getDocument(Reader r) throws DocumentException {
        SAXReader reader = null;//readerQueue.poll();
        if (reader == null) {
            reader = new SAXReader();
        }
                
        //now do actual parsing
        Document xml = null;
        
        xml = reader.read(r);
        //return the reader back to the queue
        //reader.resetHandlers();
        //readerQueue.add(reader);
        
        return xml;
    }

    public Element getChildByName(Element parent, String name) {
        if (parent == null)
            return null;

        for (Iterator<Element> i = parent.elementIterator(); i.hasNext();) {
            Element child = i.next();
            if (child.getName().equals(name)) {
                return child;
            }
        }

        return null;
    }

    public Collection<Element> getChildrenByName(Element parent,
            String name) {
        Collection<Element> children = new ArrayList<Element>();

        if (parent == null)
            return children;

        for (Iterator<Element> i = parent.elementIterator(); i.hasNext();) {
            Element child = i.next();
            if (child.getName().equals(name)) {
                children.add(child);
            }
        }
        return children;
    }
    
    public String stripNonValidXMLCharacters(String in) {
        //from http://blog.mark-mclaren.info/2007/02/invalid-xml-characters-when-valid-utf8_5873.html

        if (in == null){ 
            return null;
        }
        
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.
        
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) ||
                (current == 0xA) ||
                (current == 0xD) ||
                ((current >= 0x20) && (current <= 0xD7FF)) ||
                ((current >= 0xE000) && (current <= 0xFFFD)) ||
                ((current >= 0x10000) && (current <= 0x10FFFF))){
                out.append(current);
            }
        }
        return out.toString();
    } 

    public org.w3c.dom.Document convertDocument(Document orig) throws TransformerException {
        Transformer t = tf.newTransformer();
        DOMResult result = new DOMResult();
        t.transform(new DocumentSource(orig), result);
        return (org.w3c.dom.Document) result.getNode();
    }

    public boolean isSameXML(Document docA, Document docB) throws TransformerException {

    	//some XMLUnit config is done statically at creation
        org.w3c.dom.Document docw3cA = null;
        org.w3c.dom.Document docw3cB= null;
        docw3cA = convertDocument(docA);
        docw3cB = convertDocument(docB);
        
        Diff diff = new Diff(docw3cA, docw3cB);        
        return !diff.similar();
    }

    public void writeDocumentToFile(Document document, File outFile) throws IOException {

        OutputStream os = null;
        XMLWriter writer = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(outFile));
            //this pretty printing is messing up comparisons by trimming whitespace WITHIN an element
            //OutputFormat format = OutputFormat.createPrettyPrint();
            //XMLWriter writer = new XMLWriter(os, format);
            writer = new XMLWriter(os);
            writer.write(document);
            writer.flush();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
            
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
    }
}
