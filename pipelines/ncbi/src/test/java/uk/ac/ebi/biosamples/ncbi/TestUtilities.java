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
package uk.ac.ebi.biosamples.ncbi;

import java.io.IOException;
import java.io.InputStream;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class TestUtilities {

  public Element readNcbiBiosampleSetFromFile(String filePath)
      throws DocumentException, IOException {
    Resource resource = new ClassPathResource(filePath);
    InputStream resourceStream = resource.getInputStream();

    SAXReader reader = new SAXReader();
    return reader.read(resourceStream).getRootElement();
  }
}
