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
import uk.ac.ebi.biosamples.core.model.Curation;
import uk.ac.ebi.biosamples.core.model.CurationLink;
import uk.ac.ebi.biosamples.core.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.core.service.CustomInstantSerializer;

@Document
public class MongoCurationLink implements Comparable<MongoCurationLink> {
  @Id private final String hash;

  @Indexed(background = true)
  private final String sample;

  private final String webinSubmissionAccountId;

  @Indexed(background = true)
  protected final Instant created;

  private final Curation curation;
  private String domain;

  private MongoCurationLink(
      final String sample,
      final String domain,
      final String webinSubmissionAccountId,
      final Curation curation,
      final String hash,
      final Instant created) {
    this.sample = sample;
    this.domain = domain;
    this.webinSubmissionAccountId = webinSubmissionAccountId;
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

  public String getWebinSubmissionAccountId() {
    return webinSubmissionAccountId;
  }

  public String getHash() {
    return hash;
  }

  @JsonSerialize(using = CustomInstantSerializer.class)
  public Instant getCreated() {
    return created;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof CurationLink)) {
      return false;
    }
    final MongoCurationLink other = (MongoCurationLink) o;

    return Objects.equals(curation, other.curation) && Objects.equals(sample, other.sample);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sample, domain, webinSubmissionAccountId, curation);
  }

  @Override
  public int compareTo(final MongoCurationLink other) {
    if (other == null) {
      return 1;
    }

    if (!sample.equals(other.sample)) {
      return sample.compareTo(other.sample);
    }

    if (!curation.equals(other.curation)) {
      return curation.compareTo(other.curation);
    }

    return 0;
  }

  @Override
  public String toString() {
    final String sb = "MongoCurationLink(" + sample + "," + curation + ")";

    return sb;
  }

  // Used for deserializtion (JSON -> Java)
  @JsonCreator
  public static MongoCurationLink build(
      @JsonProperty("sample") final String sample,
      @JsonProperty("curation") final Curation curation,
      @JsonProperty("domain") final String domain,
      @JsonProperty("webinSubmissionAccountId") final String webinSubmissionAccountId,
      @JsonProperty("created") @JsonDeserialize(using = CustomInstantDeserializer.class)
          final Instant created) {
    final String hash =
        Hashing.sha256()
            .newHasher()
            .putUnencodedChars(curation.getHash())
            .putUnencodedChars(sample)
            .hash()
            .toString();

    return new MongoCurationLink(sample, domain, webinSubmissionAccountId, curation, hash, created);
  }
}
