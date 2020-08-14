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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.core.Relation;

// @JsonDeserialize(using = SampleRelationsDeserializer.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
@Relation(value = "externallinkrelations", collectionRelation = "externallinksrelations")
public
class ExternalLinksRelation { // FIXME ExternalLink relations should be mapped as entities in v4.
  // How do we handle this?
  private String url;

  public ExternalLinksRelation(String url) {
    this.url = url;
  }

  @JsonProperty
  public String url() {
    return this.url;
  }
}
