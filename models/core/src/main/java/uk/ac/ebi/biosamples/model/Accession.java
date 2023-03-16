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
package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Objects;
import uk.ac.ebi.biosamples.service.AccessionSerializer;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonSerialize(using = AccessionSerializer.class)
public class Accession implements Comparable<Accession> {
  protected String id;

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  private Accession(final String id) {
    this.id = id;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Accession)) {
      return false;
    }
    final Accession other = (Accession) o;
    return id.equalsIgnoreCase(other.id);
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

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return id;
  }

  @JsonCreator
  public static Accession build(@JsonProperty("id") final String accession) {
    return new Accession(accession);
  }
}
