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
package uk.ac.ebi.biosamples.model.certification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Plan {
  @JsonProperty(value = "candidate_checklist_id")
  private String candidateChecklistID;

  @JsonProperty(value = "certification_checklist_id")
  private String certificationChecklistID;

  private List<Curation> curations;

  public Plan(
      String candidateChecklistID, String certificationChecklistID, List<Curation> curations) {
    this.candidateChecklistID = candidateChecklistID;
    this.certificationChecklistID = certificationChecklistID;
    this.curations = curations;
  }

  private Plan() {}

  public String getID() {
    return String.format("%s -> %s", candidateChecklistID, certificationChecklistID);
  }

  public String getCandidateChecklistID() {
    return candidateChecklistID;
  }

  public void setCandidateChecklistID(String candidateChecklistID) {
    this.candidateChecklistID = candidateChecklistID;
  }

  public String getCertificationChecklistID() {
    return certificationChecklistID;
  }

  public void setCertificationChecklistID(String certificationChecklistID) {
    this.certificationChecklistID = certificationChecklistID;
  }

  public List<Curation> getCurations() {
    return curations;
  }

  public void setCurations(List<Curation> curations) {
    this.curations = curations;
  }

  public CurationResult applyCuration(SampleDocument sampleDocument, Curation curation) {
    JSONObject jsonObject = new JSONObject(sampleDocument.getDocument());
    JSONObject sampleCharacteristics = jsonObject.getJSONObject("characteristics");
    String targetCharateristic = curation.getCharacteristic();
    if (sampleCharacteristics.has(targetCharateristic)) {
      JSONArray jsonArray = sampleCharacteristics.getJSONArray(curation.getCharacteristic());
      String before = jsonArray.getJSONObject(0).getString("text");
      CurationResult curationResult =
          new CurationResult(targetCharateristic, before, curation.getValue());
      return curationResult;
    }
    return new CurationResult(targetCharateristic);
  }

  @Override
  public String toString() {
    return "Plan{"
        + "candidateChecklistID='"
        + candidateChecklistID
        + '\''
        + ", certificationChecklistID='"
        + certificationChecklistID
        + '\''
        + '}';
  }
}
