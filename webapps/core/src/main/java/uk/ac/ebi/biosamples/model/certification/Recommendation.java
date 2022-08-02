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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Recommendation {
  @JsonProperty(value = "certification_checklist_id")
  private String certificationChecklistID;

  @JsonProperty(value = "suggestions")
  private List<Suggestion> suggestions;

  @JsonPropertyOrder({"certification_checklist_id", "suggestions"})
  public Recommendation(String certificationChecklistID, List<Suggestion> suggestions) {
    this.certificationChecklistID = certificationChecklistID;
    this.suggestions = suggestions;
  }

  private Recommendation() {}

  public String getCertificationChecklistID() {
    return certificationChecklistID;
  }

  public void setCertificationChecklistID(String certificationChecklistID) {
    this.certificationChecklistID = certificationChecklistID;
  }

  public List<Suggestion> getSuggestions() {
    return suggestions;
  }

  public void setSuggestions(List<Suggestion> suggestions) {
    this.suggestions = suggestions;
  }

  public String applySuggestion(SampleDocument sampleDocument, Suggestion suggestion) {
    JSONObject jsonObject = new JSONObject(sampleDocument.getDocument());
    JSONObject sampleCharacteristics = jsonObject.getJSONObject("characteristics");
    String[] targetCharateristic = suggestion.getCharacteristic();
    AtomicBoolean mandatoryPresent = new AtomicBoolean(false);

    Arrays.asList(targetCharateristic)
        .forEach(
            characteristic -> {
              if (sampleCharacteristics.has(characteristic)) {
                mandatoryPresent.set(true);
              }
            });

    if (!mandatoryPresent.get()) {
      return "Sample not compliant with "
          + certificationChecklistID
          + " because of reason -> "
          + suggestion.getComment();
    } else {
      return "Compliant with " + certificationChecklistID;
    }
  }

  @Override
  public String toString() {
    return "Recommendation{"
        + "certificationChecklistID='"
        + certificationChecklistID
        + '\''
        + ", suggestions="
        + suggestions
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Recommendation)) return false;
    Recommendation that = (Recommendation) o;
    return Objects.equals(getCertificationChecklistID(), that.getCertificationChecklistID())
        && Objects.equals(getSuggestions(), that.getSuggestions());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCertificationChecklistID(), getSuggestions());
  }
}
