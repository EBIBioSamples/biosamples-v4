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
package uk.ac.ebi.biosamples.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.controller.SampleTabV1Controller.SampleTabRequest;

@Service
public class SampleTabMultipartFileConverter implements Converter<MultipartFile, SampleTabRequest> {
  @Override
  public SampleTabRequest convert(MultipartFile multipartFile) {
    List<List<String>> fileContent = new LinkedList<>();

    try {
      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(multipartFile.getInputStream()));
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        List<String> elements =
            Stream.of(line.split("\t"))
                .map(String::trim)
                .map(this::cleanFromEscapedCharacters)
                .collect(Collectors.toList());
        fileContent.add(elements);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return new SampleTabRequest(fileContent);
  }

  private String cleanFromEscapedCharacters(String string) {
    String resultString = string.replace("\"", "").replace("\\\\", "\\");
    return resultString;
  }
}
