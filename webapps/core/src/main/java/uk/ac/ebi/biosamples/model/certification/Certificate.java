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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Objects;

public class Certificate implements HasCuratedSample, HasChecklist {
  private final SampleDocument sampleDocument;
  private final List<CurationResult> curationsResults;
  private final Checklist checklist;

  @JsonPropertyOrder({"sampleDocument", "curationsResults", "checklist"})
  public Certificate(HasCuratedSample hasCuratedSample, Checklist checklist) {
    this(hasCuratedSample.getSampleDocument(), hasCuratedSample.getCurationResults(), checklist);
  }

  @JsonPropertyOrder({"sampleDocument", "curationsResults", "checklist"})
  public Certificate(
      SampleDocument sampleDocument, List<CurationResult> curationsResults, Checklist checklist) {
    this.sampleDocument = sampleDocument;
    this.curationsResults = curationsResults;
    this.checklist = checklist;
  }

  public SampleDocument getSampleDocument() {
    return sampleDocument;
  }

  public Checklist getChecklist() {
    return checklist;
  }

  @JsonProperty(value = "curations")
  public List<CurationResult> getCurationResults() {
    return curationsResults;
  }

  @Override
  public String toString() {
    return "Certificate{"
        + "sampleDocument="
        + sampleDocument
        + ", curationsResults="
        + curationsResults
        + ", checklist="
        + checklist
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Certificate that = (Certificate) o;
    return sampleDocument.equals(that.sampleDocument)
        && curationsResults.equals(that.curationsResults)
        && checklist.equals(that.checklist);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sampleDocument, curationsResults, checklist);
  }
}
