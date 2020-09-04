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
package uk.ac.ebi.biosamples.mongo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.hash.Hashing;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

@Document
public class MongoCurationLink implements Comparable<MongoCurationLink> {

  @Id private final String hash;

  @Indexed(background = true)
  private final String sample;

  private final String domain;

  @Indexed(background = true)
  protected final Instant created;

  private final Curation curation;

  private MongoCurationLink(
      String sample, String domain, Curation curation, String hash, Instant created) {
    this.sample = sample;
    this.domain = domain;
    this.curation = curation;
    this.hash = hash;
    this.created = created;
  }

  public String getSample() {
    return sample;
  }

  public Curation getCuration() {
    return curation;
  }

  public String getDomain() {
    return domain;
  }

  public String getHash() {
    return hash;
  }

  @JsonSerialize(using = CustomInstantSerializer.class)
  public Instant getCreated() {
    return created;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof CurationLink)) {
      return false;
    }
    MongoCurationLink other = (MongoCurationLink) o;
    return Objects.equals(this.curation, other.curation)
        && Objects.equals(this.sample, other.sample);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sample, domain, curation);
  }

  @Override
  public int compareTo(MongoCurationLink other) {
    if (other == null) {
      return 1;
    }

    if (!this.sample.equals(other.sample)) {
      return this.sample.compareTo(other.sample);
    }
    if (!this.curation.equals(other.curation)) {
      return this.curation.compareTo(other.curation);
    }
    return 0;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MongoCurationLink(");
    sb.append(this.sample);
    sb.append(",");
    sb.append(this.curation);
    sb.append(")");
    return sb.toString();
  }

  // Used for deserializtion (JSON -> Java)
  @JsonCreator
  public static MongoCurationLink build(
      @JsonProperty("sample") String sample,
      @JsonProperty("curation") Curation curation,
      @JsonProperty("domain") String domain,
      @JsonProperty("created") @JsonDeserialize(using = CustomInstantDeserializer.class)
          Instant created) {

    String hash =
        Hashing.sha256()
            .newHasher()
            .putUnencodedChars(curation.getHash())
            .putUnencodedChars(sample)
            .hash()
            .toString();
    // TODO hash on domain
    // TODO synchronized with CurationLink

    return new MongoCurationLink(sample, domain, curation, hash, created);
  }
}
