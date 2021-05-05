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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.ExternalReference;

@Service
public class ExternalReferenceService {
  public String getNickname(ExternalReference externalReference) {
    return ExternalReferenceUtils.getNickname(externalReference);
  }

  public Optional<String> getDataId(ExternalReference externalReference) {
    return ExternalReferenceUtils.getDataId(externalReference);
  }

  public String getDuoUrl(String duoCode) {
    return ExternalReferenceUtils.getDuoUrl(duoCode);
  }

  public Map<String, String> getOntologyDescription(String ontologyId) {
    Map<String, String> ontology = new HashMap<>();
    ontology.put("header", "");
    ontology.put("body", "body");

    return ontology;
  }
}
