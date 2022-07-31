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
package uk.ac.ebi.biosamples.model.structured;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

public class StructuredData {
  protected String accession;

  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  protected Instant create;

  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  protected Instant update;

  protected Set<StructuredDataTable> data;

  protected StructuredData() {}

  public String getAccession() {
    return accession;
  }

  public Instant getCreate() {
    return create;
  }

  public Instant getUpdate() {
    return update;
  }

  public Set<StructuredDataTable> getData() {
    return data;
  }

  public static StructuredData build(
      String accession, Instant create, Set<StructuredDataTable> data) {
    StructuredData structuredData = new StructuredData();
    structuredData.accession = accession;
    structuredData.create = create;
    structuredData.update = Instant.now();
    structuredData.data = data;
    return structuredData;
  }

  public static StructuredData build(
      String accession, Instant create, Instant update, Set<StructuredDataTable> data) {
    StructuredData structuredData = new StructuredData();
    structuredData.accession = accession;
    structuredData.create = create;
    structuredData.update = update;
    structuredData.data = data;
    return structuredData;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof StructuredData) {
      StructuredData o = (StructuredData) object;
      if (accession.equals(o.getAccession())) {
        return data.equals(o.getData());
      }
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accession, data);
  }

  @Override
  public String toString() {
    return "{ StructuredData: { accession: " + accession + "}}";
  }
}
