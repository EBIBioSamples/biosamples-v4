package uk.ac.ebi.biosamples.utils;

import java.io.IOException;
import java.io.InputStream;

import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * Utility class that reads an input stream of XML (with SAX) and calls a
 * provided handler for each element of interest. The handler is given a DOM
 * populated element to do something with
 * 
 * 
 * @author faulcon
 *
 */
@Service
public class XmlFragmenter {

	private SAXParserFactory factory = SAXParserFactory.newInstance();
	
	private XmlFragmenter() {};
	
	public void handleStream(InputStream inputStream, String encoding, ElementCallback callback)
			throws ParserConfigurationException, SAXException, IOException {

		InputSource isource = new InputSource(inputStream);
		isource.setEncoding(encoding);

		DefaultHandler handler = new FragmentationHandler(callback);
		SAXParser saxParser = factory.newSAXParser();
		
		saxParser.parse(isource, handler);
		
	}

	private class FragmentationHandler extends DefaultHandler {

		private final ElementCallback callback;

		private Document doc = null;
		private boolean inRegion = false;
		private Stack<Element> elementStack = new Stack<Element>();
		private StringBuilder textBuffer = new StringBuilder();

		public FragmentationHandler(ElementCallback callback) {
			this.callback = callback;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			if (callback.isBlockStart(uri, localName, qName, attributes)) {
				inRegion = true;
				doc = DocumentHelper.createDocument();

			}
			if (inRegion) {
				addTextIfNeeded();
				Element el;
				if (elementStack.size() == 0) {

					el = doc.addElement(qName);
				} else {
					el = elementStack.peek().addElement(qName);
				}
				for (int i = 0; i < attributes.getLength(); i++) {
					el.addAttribute(attributes.getQName(i), attributes.getValue(i));
				}
				elementStack.push(el);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
			if (inRegion) {
				addTextIfNeeded();
				elementStack.pop();

				if (elementStack.isEmpty()) {
					// do something with the element
					try {
						callback.handleElement(doc.getRootElement());
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

					inRegion = false;
					doc = null;

				}
			}
		}

		@Override
		public void characters(char ch[], int start, int length) throws SAXException {
			if (inRegion) {
				textBuffer.append(ch, start, length);
			}
		}

		// Outputs text accumulated under the current node
		private void addTextIfNeeded() {
			if (textBuffer.length() > 0) {
				Element el = elementStack.peek();
				el.addText(textBuffer.toString());
				textBuffer.delete(0, textBuffer.length());
			}
		}
	};

	public interface ElementCallback {
		/**
		 * This function is passed a DOM element of interest for further processing.
		 * 
		 * @param e
		 * @throws Exception 
		 */
		public void handleElement(Element e) throws Exception;

		/**
		 * This functions determines if an element is of interest and should be handled
		 * once parsing is complete.
		 * 
		 * @param uri
		 * @param localName
		 * @param qName
		 * @param attributes
		 * @return
		 */
		public boolean isBlockStart(String uri, String localName, String qName, Attributes attributes);
	}
}
