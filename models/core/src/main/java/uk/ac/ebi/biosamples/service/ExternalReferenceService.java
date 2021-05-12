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
    Map<String, String> v1 = new HashMap<>();
    v1.put("title", "Data use permission");
    v1.put("body", "A data item that is used to indicate consent permissions for datasets and/or materials, and relates to the purposes for which datasets and/or material might be removed, stored or used");
    ontologyMap.put("DUO:0000001", v1);
    Map<String, String> v2 = new HashMap<>();
    v2.put("title", "Obsolete general research use and clinical care");
    v2.put("body", "This data use limitation indicates that use is allowed for health/medical/biomedical purposes and other biological research, including the study of population origins or ancestry");
    ontologyMap.put("DUO:0000005", v2);
    Map<String, String> v3 = new HashMap<>();
    v3.put("title", "Disease specific research");
    v3.put("body", "This term should be coupled with a term describing a disease from an ontology to specify the disease the restriction applies to");
    ontologyMap.put("DUO:0000007", v3);
    Map<String, String> v4 = new HashMap<>();
    v4.put("title", "Obsolete research use only");
    v4.put("body", "This data use limitation indicates that use is limited to research purposes");
    ontologyMap.put("DUO:0000014", v4);
    Map<String, String> v5 = new HashMap<>();
    v5.put("title", "Publication required");
    v5.put("body", "This data use modifier indicates that requestor agrees to make results of studies using the data available to the larger scientific community");
    ontologyMap.put("DUO:0000019", v5);
    Map<String, String> v6 = new HashMap<>();
    v6.put("title", "User specific restriction");
    v6.put("body", "This data use modifier indicates that use is limited to use by approved users.");
    ontologyMap.put("DUO:0000026", v6);
    Map<String, String> v7 = new HashMap<>();
    v7.put("title", "Institution specific restriction");
    v7.put("body", "This data use modifier indicates that use is limited to use within an approved institution");
    ontologyMap.put("DUO:0000028", v7);
  }
}
