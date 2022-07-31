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
package uk.ac.ebi.biosamples.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * Utility class that reads an input stream of XML (with SAX) and calls a provided handler for each
 * element of interest. The handler is given a DOM populated element to do something with
 *
 * @author faulcon
 */
@Service
public class XmlFragmenter {
  private SAXParserFactory factory = SAXParserFactory.newInstance();

  private XmlFragmenter() {}

  public void handleStream(InputStream inputStream, String encoding, ElementCallback... callback)
      throws ParserConfigurationException, SAXException, IOException {

    InputSource isource = new InputSource(inputStream);
    isource.setEncoding(encoding);

    DefaultHandler handler = new FragmentationHandler(callback);
    SAXParser saxParser = factory.newSAXParser();

    saxParser.parse(isource, handler);
  }

  private class FragmentationHandler extends DefaultHandler {
    private final List<ElementCallback> callbacks;
    private final List<Document> doc;
    private final List<Boolean> inRegion;
    private final List<Stack<Element>> elementStack;
    private final List<StringBuilder> textBuffer;

    public FragmentationHandler(ElementCallback... callbacks) {
      this.callbacks = Arrays.asList(callbacks);

      this.doc = new ArrayList<>(this.callbacks.size());
      this.inRegion = new ArrayList<>(this.callbacks.size());
      this.elementStack = new ArrayList<>(this.callbacks.size());
      this.textBuffer = new ArrayList<>(this.callbacks.size());

      for (ElementCallback callback : callbacks) {
        doc.add(null);
        inRegion.add(false);
        elementStack.add(new Stack<>());
        textBuffer.add(new StringBuilder());
      }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
      for (int i = 0; i < callbacks.size(); i++) {
        if (callbacks.get(i).isBlockStart(uri, localName, qName, attributes)) {
          inRegion.set(i, true);
          doc.set(i, DocumentHelper.createDocument());
        }
        if (inRegion.get(i)) {
          addTextIfNeeded(i);
          Element el;
          if (elementStack.get(i).size() == 0) {
            el = doc.get(i).addElement(qName);
          } else {
            el = elementStack.get(i).peek().addElement(qName);
          }
          for (int j = 0; j < attributes.getLength(); j++) {
            el.addAttribute(attributes.getQName(j), attributes.getValue(j));
          }
          elementStack.get(i).push(el);
        }
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      for (int i = 0; i < callbacks.size(); i++) {
        if (inRegion.get(i)) {
          addTextIfNeeded(i);
          elementStack.get(i).pop();

          if (elementStack.get(i).isEmpty()) {
            // do something with the element
            try {
              callbacks.get(i).handleElement(doc.get(i).getRootElement());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }

            inRegion.set(i, false);
            doc.set(i, null);
          }
        }
      }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
      for (int i = 0; i < callbacks.size(); i++) {
        if (inRegion.get(i)) {
          textBuffer.get(i).append(ch, start, length);
        }
      }
    }

    // Outputs text accumulated under the current node
    private void addTextIfNeeded(int i) {
      if (textBuffer.get(i).length() > 0) {
        Element el = elementStack.get(i).peek();
        el.addText(textBuffer.get(i).toString());
        textBuffer.get(i).delete(0, textBuffer.get(i).length());
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
     * This functions determines if an element is of interest and should be handled once parsing is
     * complete.
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
