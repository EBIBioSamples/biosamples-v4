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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.Charset;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

public class TestUtilities {

  public static String readFileAsString(String pathToFile) {

    String fileContent;

    try {
      fileContent =
          StreamUtils.copyToString(
              new ClassPathResource(pathToFile).getInputStream(), Charset.defaultCharset());
    } catch (IOException e) {
      throw new RuntimeException("An error occurred while reading resource " + pathToFile, e);
    }

    return fileContent;
  }

  public static JsonNode readJsonDocument(String pathToFile) {
    ObjectMapper mapper = new ObjectMapper();

    String fileContent = TestUtilities.readFileAsString(pathToFile);

    try {
      return mapper.readTree(fileContent);
    } catch (IOException e) {
      throw new RuntimeException(
          "An error occurred while converting the string " + fileContent + " to a JSON object", e);
    }
  }

  public static Document readXmlDocument(String pathToFile) {
    String fileContent = TestUtilities.readFileAsString(pathToFile);
    try {
      return DocumentHelper.parseText(fileContent);
    } catch (DocumentException exp) {
      throw new RuntimeException(exp);
    }
  }
}
