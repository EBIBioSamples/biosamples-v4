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
package uk.ac.ebi.biosamples;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class NcbiTestsService {

  public static Element readNcbiBiosampleElementFromFile(String pathToFile) {
    try {
      InputStream xmlInputStream = NcbiTestsService.class.getResourceAsStream(pathToFile);
      String xmlDocument =
          new BufferedReader(new InputStreamReader(xmlInputStream))
              .lines()
              .collect(Collectors.joining());
      Document doc = DocumentHelper.parseText(xmlDocument);
      return doc.getRootElement().element("BioSample");
    } catch (DocumentException exp) {
      throw new RuntimeException(exp);
    }
  }

  public static List<Element> readNcbiBioSampleElementsFromFile(String pathToFile) {
    try {
      InputStream xmlInputStream = NcbiTestsService.class.getResourceAsStream(pathToFile);
      String xmlDocument =
          new BufferedReader(new InputStreamReader(xmlInputStream))
              .lines()
              .collect(Collectors.joining());
      Document doc = DocumentHelper.parseText(xmlDocument);
      return XmlPathBuilder.of(doc.getRootElement()).elements("BioSample");
    } catch (DocumentException exp) {
      throw new RuntimeException(exp);
    }
  }
}
