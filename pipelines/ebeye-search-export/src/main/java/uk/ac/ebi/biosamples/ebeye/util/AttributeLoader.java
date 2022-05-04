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
package uk.ac.ebi.biosamples.ebeye.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class AttributeLoader {
  private Set<String> attributeSet = new HashSet<>();

  public Set<String> getAllAttributes() throws IOException {
    final InputStream resource =
        new ClassPathResource("most_common_attributes.list").getInputStream();
    final Scanner scanner = new Scanner(resource);
    final List<String> list = new ArrayList<>();

    while (scanner.hasNextLine()) {
      list.add(scanner.nextLine());
    }

    scanner.close();

    list.forEach(
        element -> {
          attributeSet.add(Arrays.asList(element.split(",")).get(0));
        });

    return attributeSet;
  }
}
