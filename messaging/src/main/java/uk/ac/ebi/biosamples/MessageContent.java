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
package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageContent {
  private final Sample sample;
  private final CurationLink curationLink;
  private final List<Sample> related;
  private final boolean delete;
  private final String creationTime;

  private MessageContent(
      final Sample sample,
      final CurationLink curationLink,
      final List<Sample> related,
      final boolean delete) {
    this.sample = sample;
    this.curationLink = curationLink;
    this.related = related;
    this.delete = delete;
    creationTime = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
  }

  public Sample getSample() {
    return sample;
  }

  public List<Sample> getRelated() {
    return related;
  }

  public CurationLink getCurationLink() {
    return curationLink;
  }

  public String getCreationTime() {
    return creationTime;
  }

  @Override
  public String toString() {
    final String sb =
        "MessageContent(" + sample + "," + curationLink + "," + related + "," + delete + ")";
    return sb;
  }

  @JsonCreator
  public static MessageContent build(
      @JsonProperty("sample") final Sample sample,
      @JsonProperty("curationLink") final CurationLink curationLink,
      @JsonProperty("related") final List<Sample> related,
      @JsonProperty("delete") final boolean delete) {
    return new MessageContent(sample, curationLink, related, delete);
  }
}
