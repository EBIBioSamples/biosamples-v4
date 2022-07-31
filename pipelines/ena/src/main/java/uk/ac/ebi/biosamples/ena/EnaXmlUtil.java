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
package uk.ac.ebi.biosamples.ena;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnaXmlUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnaXmlUtil.class);

  public static String pretty(String xmlString) {
    SAXReader reader = new SAXReader();
    Document document = null;
    try {
      document = reader.read(new StringReader(xmlString));
    } catch (DocumentException e) {
      LOGGER.error("Error reading XML", e);
    }
    return pretty(document);
  }

  public static String pretty(Document document) {
    OutputFormat format = OutputFormat.createPrettyPrint();
    StringWriter outputWriter = new StringWriter();
    XMLWriter writer = new XMLWriter(outputWriter, format);
    try {
      writer.write(document);
      outputWriter.close();
      writer.close();
    } catch (IOException e) {
      LOGGER.error("Error writing XML", e);
    }
    return outputWriter.toString();
  }
}
