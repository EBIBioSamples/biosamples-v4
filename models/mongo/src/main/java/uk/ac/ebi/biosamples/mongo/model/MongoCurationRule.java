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
import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

@Document
public class MongoCurationRule implements Comparable<MongoCurationRule> {

  @Id private String id;
  private String attributePre;
  private String attributePost;
  @JsonSerialize(using = CustomInstantSerializer.class)
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  private final Instant created;

  private MongoCurationRule(String attributePre, String attributePost) {
    this.attributePre = attributePre;
    this.attributePost = attributePost;
    id = attributePre;
    created = Instant.now();
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("attributePre")
  public String getAttributePre() {
    return attributePre;
  }

  @JsonProperty("attributePost")
  public String getAttributePost() {
    return attributePost;
  }

  @JsonProperty("created")
  public Instant getCreated() {
    return created;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributePre);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof MongoCurationRule)) {
      return false;
    }
    MongoCurationRule other = (MongoCurationRule) o;
    return Objects.equals(this.attributePre, other.attributePre)
        && Objects.equals(this.attributePost, other.attributePost)
        && Objects.equals(this.created, other.created);
  }

  @Override
  public int compareTo(MongoCurationRule other) {
    if (other == null) {
      return 1;
    }

    if (!this.attributePre.equals(other.attributePre)) {
      return this.attributePre.compareTo(other.attributePre);
    } else if (!this.attributePost.equals(other.attributePost)) {
      return this.attributePost.compareTo(other.attributePost);
    } else if (!this.created.equals(other.created)) {
      return this.created.compareTo(other.created);
    }

    return 0;
  }

  @Override
  public String toString() {
    String sb = "MongoCurationRule(" + attributePre + "," + attributePost + "," + created + ")";
    return sb;
  }

  @JsonCreator
  public static MongoCurationRule build(
      @JsonProperty("attributePre") String attributePre,
      @JsonProperty("attributePost") String attributePost) {
    return new MongoCurationRule(attributePre, attributePost);
  }
}
