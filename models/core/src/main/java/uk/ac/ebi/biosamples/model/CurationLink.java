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
import lombok.Data;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

@Data
public class CurationLink implements Comparable<CurationLink> {
  private final Curation curation;
  private final String sample;
  private final String webinSubmissionAccountId;
  private final String hash;
  private String domain;

  @JsonSerialize(using = CustomInstantSerializer.class)
  protected final Instant created;

  private CurationLink(
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

  @Override
  public int compareTo(final CurationLink other) {
    if (other == null) {
      return 1;
    }

    if (!domain.equals(other.domain)) {
      return domain.compareTo(other.domain);
    }

    if (!webinSubmissionAccountId.equals(other.webinSubmissionAccountId)) {
      return webinSubmissionAccountId.compareTo(other.webinSubmissionAccountId);
    }

    if (!sample.equals(other.sample)) {
      return sample.compareTo(other.sample);
    }

    if (!curation.equals(other.curation)) {
      return curation.compareTo(other.curation);
    }

    return 0;
  }

  // Used for deserialization (JSON -> Java)
  @JsonCreator
  public static CurationLink build(
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

    return new CurationLink(sample, domain, webinSubmissionAccountId, curation, hash, created);
  }
}
