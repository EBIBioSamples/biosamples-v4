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
package uk.ac.ebi.biosamples.mongo.model;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "mongoSampleMessage")
public class MongoSampleMessage {
  @Id @Indexed private final String biosampleAccession;
  private final Instant updateDate;
  private final Long taxId;

  public MongoSampleMessage(
      final String biosampleAccession, final Instant updateDate, final Long taxId) {
    this.biosampleAccession = biosampleAccession;
    this.updateDate = updateDate;
    this.taxId = taxId;
  }

  private String getBiosampleAccession() {
    return biosampleAccession;
  }

  public Long getTaxId() {
    return taxId;
  }

  public Instant getUpdateDate() {
    return updateDate;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MongoSampleMessage)) {
      return false;
    }
    final MongoSampleMessage that = (MongoSampleMessage) o;
    return Objects.equals(getBiosampleAccession(), that.getBiosampleAccession())
        && Objects.equals(getUpdateDate(), that.getUpdateDate())
        && Objects.equals(getTaxId(), that.getTaxId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBiosampleAccession(), getUpdateDate(), getTaxId());
  }

  @Override
  public String toString() {
    return "MongoSampleMessage{"
        + "biosampleAccession='"
        + biosampleAccession
        + '\''
        + ", updateDate="
        + updateDate
        + ", taxId="
        + taxId
        + '}';
  }
}
