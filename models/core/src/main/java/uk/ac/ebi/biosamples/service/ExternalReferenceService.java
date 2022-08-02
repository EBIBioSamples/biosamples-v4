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
    Map<String, String> ontology = new HashMap<>();
    ontology.put("title", "Data use permission");
    ontology.put(
        "body",
        "A data item that is used to indicate consent permissions for datasets and/or materials, and relates to the purposes for which datasets and/or material might be removed, stored or used");
    ontologyMap.put("DUO:0000001", ontology);
    ontology = new HashMap<>();
    ontology.put("title", "No restriction");
    ontology.put("body", "This data use permission indicates there is no restriction on use.");
    ontologyMap.put("DUO:0000004", ontology);
    ontology = new HashMap<>();
    ontology.put("title", "General research use and clinical care (obsolete)");
    ontology.put(
        "body",
        "This data use limitation indicates that use is allowed for health/medical/biomedical purposes and other biological research, including the study of population origins or ancestry");
    ontologyMap.put("DUO:0000005", ontology);
    ontology = new HashMap<>();
    ontology.put("title", "Health/Medical/Biomedical research and clinical care");
    ontology.put(
        "body",
        "Use of the data is limited to health/medical/biomedical purposes; does not include the study of populations origin or ancestry");
    ontologyMap.put("DUO:0000006", ontology);
    ontology = new HashMap<>();
    ontology.put("title", "Disease specific research");
    ontology.put(
        "body",
        "This term should be coupled with a term describing a disease from an ontology to specify the disease the restriction applies to");
    ontologyMap.put("DUO:0000007", ontology);
    ontology = new HashMap<>();
    ontology.put("title", "Research use only (obsolete)");
    ontology.put(
        "body", "This data use limitation indicates that use is limited to research purposes");
    ontologyMap.put("DUO:0000014", ontology);
    ontology = new HashMap<>();
    ontology.put("title", "Publication required");
    ontology.put(
        "body",
        "This data use modifier indicates that requestor agrees to make results of studies using the data available to the larger scientific community");
    ontologyMap.put("DUO:0000018", ontology);
    ontology = new HashMap<>();
    ontology.put("title", "Not-for-profit use only");
    ontology.put("body", "Use of the data is limited to not-for-profit organizations");
    ontologyMap.put("DUO:0000019", ontology);
    ontology = new HashMap<>();
    ontology.put("title", "User specific restriction");
    ontology.put(
        "body", "This data use modifier indicates that use is limited to use by approved users.");
    ontologyMap.put("DUO:0000026", ontology);
    ontology = new HashMap<>();
    ontology.put("title", "Institution specific restriction");
    ontology.put(
        "body",
        "This data use modifier indicates that use is limited to use within an approved institution");
    ontologyMap.put("DUO:0000028", ontology);
  }
}
