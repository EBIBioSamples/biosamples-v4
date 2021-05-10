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
  private final Map<String, Map<String, String>> ontologyMap;

  public ExternalReferenceService() {
    ontologyMap = new HashMap<>();
    populateOntologies(ontologyMap);
  }

  public String getNickname(ExternalReference externalReference) {
    return ExternalReferenceUtils.getNickname(externalReference);
  }

  public Optional<String> getDataId(ExternalReference externalReference) {
    return ExternalReferenceUtils.getDataId(externalReference);
  }

  public String getDuoUrl(String duoCode) {
    return ExternalReferenceUtils.getDuoUrl(duoCode);
  }

  public String getOntologyTitle(String ontologyId) {
    return ontologyMap.containsKey(ontologyId) ? ontologyMap.get(ontologyId).get("title") : "";
  }

  public String getOntologyDescription(String ontologyId) {
    return ontologyMap.containsKey(ontologyId) ? ontologyMap.get(ontologyId).get("body") : "";
  }

  private void populateOntologies(Map<String, Map<String, String>> ontologyMap) {
    ontologyMap.put("DUO:0000001", Map.of("title", "Data use permission", "body", "A data item that is used to indicate consent permissions for datasets and/or materials, and relates to the purposes for which datasets and/or material might be removed, stored or used"));
    ontologyMap.put("DUO:0000005", Map.of("title", "Obsolete general research use and clinical care", "body", "This data use limitation indicates that use is allowed for health/medical/biomedical purposes and other biological research, including the study of population origins or ancestry"));
    ontologyMap.put("DUO:0000007", Map.of("title", "Disease specific research", "body", "This term should be coupled with a term describing a disease from an ontology to specify the disease the restriction applies to"));
    ontologyMap.put("DUO:0000014", Map.of("title", "Obsolete research use only", "body", "This data use limitation indicates that use is limited to research purposes"));
    ontologyMap.put("DUO:0000019", Map.of("title", "Publication required", "body", "This data use modifier indicates that requestor agrees to make results of studies using the data available to the larger scientific community"));
    ontologyMap.put("DUO:0000026", Map.of("title", "User specific restriction", "body", "This data use modifier indicates that use is limited to use by approved users."));
    ontologyMap.put("DUO:0000028", Map.of("title", "Institution specific restriction", "body", "This data use modifier indicates that use is limited to use within an approved institution"));
  }
}
