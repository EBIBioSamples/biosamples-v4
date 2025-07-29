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
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.util.Objects;
import org.springframework.data.mongodb.core.index.Indexed;

public class MongoRelationship implements Comparable<MongoRelationship> {

  private final String hash;

  private final String type;

  @Indexed(background = true, sparse = true)
  private final String target;

  private final String source;

  private MongoRelationship(
      final String type, final String target, final String source, final String hash) {
    this.type = type;
    this.target = target;
    this.source = source;
    this.hash = hash;
  }

  public String getType() {
    return type;
  }

  public String getTarget() {
    return target;
  }

  public String getSource() {
    return source;
  }

  public String getHash() {
    return hash;
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }
    if (!(o instanceof MongoRelationship)) {
      return false;
    }
    final MongoRelationship other = (MongoRelationship) o;
    return Objects.equals(type, other.type)
        && Objects.equals(target, other.target)
        && Objects.equals(source, other.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, target, source);
  }

  @Override
  public int compareTo(final MongoRelationship other) {
    if (other == null) {
      return 1;
    }

    if (!Objects.equals(type, other.type)) {
      return type.compareTo(other.type);
    }

    if (!Objects.equals(target, other.target)) {
      return target.compareTo(other.target);
    }

    if (!Objects.equals(source, other.source)) {
      if (source == null) {
        return 1;
      } else if (other.source == null) {
        return -1;
      } else {
        return source.compareTo(other.source);
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    final String sb = "Relationships(" + source + "," + type + "," + target + ")";
    return sb;
  }

  @JsonCreator
  public static MongoRelationship build(
      @JsonProperty("source") final String source,
      @JsonProperty("type") final String type,
      @JsonProperty("target") final String target) {
    if (type == null || type.trim().length() == 0) {
      throw new IllegalArgumentException("type cannot be empty");
    }
    if (target == null || target.trim().length() == 0) {
      throw new IllegalArgumentException("target cannot be empty");
    }

    final Hasher hasher = Hashing.sha256().newHasher();
    hasher.putUnencodedChars(type);
    hasher.putUnencodedChars(target);
    if (source != null) {
      hasher.putUnencodedChars(source);
    }

    final String hash = hasher.hash().toString();

    return new MongoRelationship(type, target, source, hash);
  }
}
