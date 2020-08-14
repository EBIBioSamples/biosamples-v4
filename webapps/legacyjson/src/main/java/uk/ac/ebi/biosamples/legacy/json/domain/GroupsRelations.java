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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.biosamples.legacy.json.service.SampleRelationsDeserializer;
import uk.ac.ebi.biosamples.model.Sample;

@JsonDeserialize(using = SampleRelationsDeserializer.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
@Relation(value = "grouprelations", collectionRelation = "groupsrelations")
public class GroupsRelations implements Relations {

  private Sample sample;

  public GroupsRelations(Sample sample) {
    this.sample = sample;
  }

  @JsonProperty
  public String accession() {
    return this.sample.getAccession();
  }

  @JsonIgnore
  public Sample getAssociatedSample() {
    return this.sample;
  }
}
