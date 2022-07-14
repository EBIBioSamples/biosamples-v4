/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class XmlUtils {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  private TransformerFactory tf = TransformerFactory.newInstance();

  static {
    XMLUnit.setIgnoreAttributeOrder(true);
    XMLUnit.setIgnoreWhitespace(true);
  }

  public Document getDocument(File xmlFile) throws FileNotFoundException, DocumentException {
    return getDocument(new BufferedReader(new FileReader(xmlFile)));
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
          // do nothing
        }
      }
    }
    return doc;
  }

  public Document getDocument(Reader r) throws DocumentException {
    SAXReader reader = null; // readerQueue.poll();
    if (reader == null) {
      reader = new SAXReader();
    }

    // now do actual parsing
    Document xml = null;

    xml = reader.read(r);
    // return the reader back to the queue
    // reader.resetHandlers();
    // readerQueue.add(reader);

    return xml;
  }

  public String stripNonValidXMLCharacters(String in) {
    // from
    // http://blog.mark-mclaren.info/2007/02/invalid-xml-characters-when-valid-utf8_5873.html

    if (in == null) {
      return null;
    }

    StringBuffer out = new StringBuffer(); // Used to hold the output.
    char current; // Used to reference the current character.

    for (int i = 0; i < in.length(); i++) {
      current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not
      // happen.
      if ((current == 0x9)
          || (current == 0xA)
          || (current == 0xD)
          || ((current >= 0x20) && (current <= 0xD7FF))
          || ((current >= 0xE000) && (current <= 0xFFFD))
          || ((current >= 0x10000) && (current <= 0x10FFFF))) {
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

    // some XMLUnit config is done statically at creation
    org.w3c.dom.Document docw3cA = null;
    org.w3c.dom.Document docw3cB = null;
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
      // this pretty printing is messing up comparisons by trimming whitespace WITHIN an
      // element
      // OutputFormat format = OutputFormat.createPrettyPrint();
      // XMLWriter writer = new XMLWriter(os, format);
      writer = new XMLWriter(os);
      writer.write(document);
      writer.flush();
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          // do nothing
        }
      }

      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          // do nothing
        }
      }
    }
  }
}
