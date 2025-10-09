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
package uk.ac.ebi.biosamples.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import uk.ac.ebi.biosamples.core.service.AccessionSerializer;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonSerialize(using = AccessionSerializer.class)
public class Accession implements Comparable<Accession> {
  @JsonProperty("id")
  protected String id;

  private Accession(final String id) {
    this.id = id;
  }

  @Override
  public int compareTo(final Accession other) {
    if (other == null) {
      return 1;
    }

    if (!id.equals(other.id)) {
      return id.compareTo(other.id);
    }

    return 0;
  }

  @JsonCreator
  public static Accession build(@JsonProperty("id") final String accession) {
    return new Accession(accession);
  }
}
