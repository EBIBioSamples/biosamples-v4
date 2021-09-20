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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.hash.Hashing;
import java.time.Instant;
import java.util.Objects;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

public class CurationLink implements Comparable<CurationLink> {

  private final Curation curation;
  private final String sample;
  private final String domain;
  private final String webinSubmissionAccountId;
  private final String hash;
  protected final Instant created;

  private CurationLink(
      String sample,
      String domain,
      String webinSubmissionAccountId,
      Curation curation,
      String hash,
      Instant created) {
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

  public String getDomain() {
    return domain;
  }

  public String getWebinSubmissionAccountId() {
    return webinSubmissionAccountId;
  }

  public Curation getCuration() {
    return curation;
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
    CurationLink other = (CurationLink) o;

    return Objects.equals(this.curation, other.curation)
        && Objects.equals(this.sample, other.sample)
        && Objects.equals(this.domain, other.domain)
        && Objects.equals(this.webinSubmissionAccountId, other.webinSubmissionAccountId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sample, domain, webinSubmissionAccountId, curation);
  }

  @Override
  public int compareTo(CurationLink other) {
    if (other == null) {
      return 1;
    }

    if (!this.domain.equals(other.domain)) {
      return this.domain.compareTo(other.domain);
    }
    if (!this.webinSubmissionAccountId.equals(other.webinSubmissionAccountId)) {
      return this.webinSubmissionAccountId.compareTo(other.webinSubmissionAccountId);
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
    sb.append("CurationLink(");
    sb.append(this.sample);
    sb.append(",");
    sb.append(this.domain);
    sb.append(",");
    sb.append(this.webinSubmissionAccountId);
    sb.append(",");
    sb.append(this.curation);
    sb.append(")");
    return sb.toString();
  }

  // Used for deserializtion (JSON -> Java)
  @JsonCreator
  public static CurationLink build(
      @JsonProperty("sample") String sample,
      @JsonProperty("curation") Curation curation,
      @JsonProperty("domain") String domain,
      @JsonProperty("webinSubmissionAccountId") String webinSubmissionAccountId,
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
    // TODO synchronized with MongoCurationLink

    return new CurationLink(sample, domain, webinSubmissionAccountId, curation, hash, created);
  }
}
