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
package uk.ac.ebi.biosamples.legacy.json.domain;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;

@JsonPropertyOrder(value = {"name", "acc", "url"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LegacyExternalReference {

  private String name;
  private String url;
  private String accession;

  public LegacyExternalReference() {}

  public LegacyExternalReference(ExternalReference externalReference) {
    ExternalReferenceService service = new ExternalReferenceService();
    this.name = service.getNickname(externalReference);
    this.accession = service.getDataId(externalReference).orElse("");
    this.url = externalReference.getUrl();
  }

  @JsonGetter
  public String getName() {
    return name;
  }

  @JsonGetter
  public String getUrl() {
    return url;
  }

  @JsonGetter("acc")
  public String getAccession() {
    return accession;
  }
}
