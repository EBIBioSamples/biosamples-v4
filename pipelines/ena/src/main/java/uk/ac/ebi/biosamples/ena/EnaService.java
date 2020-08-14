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
package uk.ac.ebi.biosamples.ena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EnaService {

  public Collection<String> splitIdentifiers(String input) {
    // split by commas
    List<String> idents = new ArrayList<String>();
    if (input.contains(",")) {
      for (String substr : input.split(",")) {
        idents.add(substr);
      }
    } else {
      idents.add(input);
    }

    // convert hyphenated ranges into separate numbers
    List<String> newidents = new ArrayList<String>();
    for (String ident : idents) {
      if (ident.contains("-")) {
        // its a range
        String[] range = ident.split("-");
        int lower = new Integer(range[0].substring(3));
        int upper = new Integer(range[1].substring(3));
        String prefix = range[0].substring(0, 3);
        for (int i = lower; i <= upper; i++) {
          newidents.add(String.format(prefix + "%06d", i));
        }
      } else {
        newidents.add(ident);
      }
    }

    // sort it before returning
    Collections.sort(newidents);

    return newidents;
  }
}
