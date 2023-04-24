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
      final String candidateChecklistID,
      final String certificationChecklistID,
      final List<Curation> curations) {
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

  public void setCandidateChecklistID(final String candidateChecklistID) {
    this.candidateChecklistID = candidateChecklistID;
  }

  public String getCertificationChecklistID() {
    return certificationChecklistID;
  }

  public void setCertificationChecklistID(final String certificationChecklistID) {
    this.certificationChecklistID = certificationChecklistID;
  }

  public List<Curation> getCurations() {
    return curations;
  }

  public void setCurations(final List<Curation> curations) {
    this.curations = curations;
  }

  public CurationResult applyCuration(
      final SampleDocument sampleDocument, final Curation curation) {
    final JSONObject jsonObject = new JSONObject(sampleDocument.getDocument());
    final JSONObject sampleCharacteristics = jsonObject.getJSONObject("characteristics");
    final String targetCharateristic = curation.getCharacteristic();
    if (sampleCharacteristics.has(targetCharateristic)) {
      final JSONArray jsonArray = sampleCharacteristics.getJSONArray(curation.getCharacteristic());
      final String before = jsonArray.getJSONObject(0).getString("text");
      final CurationResult curationResult =
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
