/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.Element;

/**
 * A fluent-style builder for interrogating XML documents and elements
 *
 * @author faulcon
 */
public class XmlPathBuilder {

  private List<String> pathParts = new ArrayList<>();
  private Element root = null;

  protected XmlPathBuilder(Element root) {
    this.root = root;
  }

  public XmlPathBuilder path(String... pathParts) {
    this.pathParts.addAll(Arrays.asList(pathParts));
    return this;
  }

  public Element element() {
    Element target = root;
    for (String pathPart : pathParts) {
      target = target.element(pathPart);
      if (target == null) {
        throw new IllegalArgumentException("Path path " + pathPart + " does not exist");
      }
    }
    return target;
  }

  public List<Element> elements() {
    return elements(null);
  }

  @SuppressWarnings("unchecked")
  public List<Element> elements(String name) {
    List<Element> elements = new ArrayList<>();
    for (Iterator<Element> i = element().elementIterator(); i.hasNext(); ) {
      Element child = i.next();
      if (name == null || child.getName().equals(name)) {
        elements.add(child);
      }
    }
    return elements;
  }

  public String text() {
    return element().getTextTrim();
  }

  public boolean exists() {
    try {
      element();
    } catch (IllegalArgumentException e) {
      return false;
    }
    return true;
  }

  public String attribute(String name) {
    if (element().attribute(name) == null) {
      throw new IllegalArgumentException(
          "Argument " + name + " does not exist at path " + String.join("/", pathParts));
    }
    return element().attributeValue(name);
  }

  public boolean attributeExists(String name) {
    if (element().attribute(name) == null) {
      return false;
    }
    return true;
  }

  public static XmlPathBuilder of(Document doc) {
    return new XmlPathBuilder(doc.getRootElement());
  }

  public static XmlPathBuilder of(Element root) {
    return new XmlPathBuilder(root);
  }
}
