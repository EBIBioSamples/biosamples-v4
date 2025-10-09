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
import java.util.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.ExternalReference;
import uk.ac.ebi.biosamples.core.model.Relationship;

@Document
public class MongoCuration implements Comparable<MongoCuration> {

  private final SortedSet<Attribute> attributesPre;
  private final SortedSet<Attribute> attributesPost;

  private final SortedSet<ExternalReference> externalPre;
  private final SortedSet<ExternalReference> externaPost;

  private final SortedSet<Relationship> relationshipsPre;
  private final SortedSet<Relationship> relationshipsPost;

  @Id private String hash;

  private MongoCuration(
      final Collection<Attribute> attributesPre,
      final Collection<Attribute> attributesPost,
      final Collection<ExternalReference> externalPre,
      final Collection<ExternalReference> externaPost,
      final Collection<Relationship> relationshipsPre,
      final Collection<Relationship> relationshipsPost,
      final String hash) {
    this.attributesPre = Collections.unmodifiableSortedSet(new TreeSet<>(attributesPre));
    this.attributesPost = Collections.unmodifiableSortedSet(new TreeSet<>(attributesPost));
    this.externalPre = Collections.unmodifiableSortedSet(new TreeSet<>(externalPre));
    this.externaPost = Collections.unmodifiableSortedSet(new TreeSet<>(externaPost));
    this.relationshipsPre =
        relationshipsPre != null
            ? Collections.unmodifiableSortedSet(new TreeSet<>(relationshipsPre))
            : Collections.unmodifiableSortedSet(new TreeSet<>());
    this.relationshipsPost =
        relationshipsPost != null
            ? Collections.unmodifiableSortedSet(new TreeSet<>(relationshipsPost))
            : Collections.unmodifiableSortedSet(new TreeSet<>());
    this.hash = hash;
  }

  @JsonProperty("attributesPre")
  public SortedSet<Attribute> getAttributesPre() {
    return attributesPre;
  }

  @JsonProperty("attributesPost")
  public SortedSet<Attribute> getAttributesPost() {
    return attributesPost;
  }

  @JsonProperty("externalReferencesPre")
  public SortedSet<ExternalReference> getExternalReferencesPre() {
    return externalPre;
  }

  @JsonProperty("externalReferencesPost")
  public SortedSet<ExternalReference> getExternalReferencesPost() {
    return externaPost;
  }

  @JsonProperty("relationshipsPre")
  public SortedSet<Relationship> getRelationshipsPre() {
    return relationshipsPre;
  }

  @JsonProperty("relationshipsPost")
  public SortedSet<Relationship> getRelationshipsPost() {
    return relationshipsPost;
  }

  public String getHash() {
    return hash;
  }

  @Override
  public int hashCode() {
    return Objects.hash(hash);
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof MongoCuration)) {
      return false;
    }
    final MongoCuration other = (MongoCuration) o;
    return Objects.equals(hash, other.hash)
        && Objects.equals(attributesPre, other.attributesPre)
        && Objects.equals(attributesPost, other.attributesPost)
        && Objects.equals(externalPre, other.externalPre)
        && Objects.equals(externaPost, other.externaPost)
        && Objects.equals(relationshipsPre, other.relationshipsPre)
        && Objects.equals(relationshipsPost, other.relationshipsPost);
  }

  @Override
  public int compareTo(final MongoCuration other) {
    if (other == null) {
      return 1;
    }

    if (!attributesPre.equals(other.attributesPre)) {
      if (attributesPre.size() < other.attributesPre.size()) {
        return -1;
      } else if (attributesPre.size() > other.attributesPre.size()) {
        return 1;
      } else {
        final Iterator<Attribute> thisIt = attributesPre.iterator();
        final Iterator<Attribute> otherIt = other.attributesPre.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }
    if (!attributesPost.equals(other.attributesPost)) {
      if (attributesPost.size() < other.attributesPost.size()) {
        return -1;
      } else if (attributesPost.size() > other.attributesPost.size()) {
        return 1;
      } else {
        final Iterator<Attribute> thisIt = attributesPost.iterator();
        final Iterator<Attribute> otherIt = other.attributesPost.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }

    if (!externalPre.equals(other.externalPre)) {
      if (externalPre.size() < other.externalPre.size()) {
        return -1;
      } else if (externalPre.size() > other.externalPre.size()) {
        return 1;
      } else {
        final Iterator<ExternalReference> thisIt = externalPre.iterator();
        final Iterator<ExternalReference> otherIt = other.externalPre.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }
    if (!externaPost.equals(other.externaPost)) {
      if (externaPost.size() < other.externaPost.size()) {
        return -1;
      } else if (externaPost.size() > other.externaPost.size()) {
        return 1;
      } else {
        final Iterator<ExternalReference> thisIt = externaPost.iterator();
        final Iterator<ExternalReference> otherIt = other.externaPost.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }

    if (!relationshipsPre.equals(other.relationshipsPre)) {
      if (relationshipsPre.size() < other.relationshipsPre.size()) {
        return -1;
      } else if (relationshipsPre.size() > other.relationshipsPre.size()) {
        return 1;
      } else {
        final Iterator<Relationship> thisIt = relationshipsPre.iterator();
        final Iterator<Relationship> otherIt = other.relationshipsPre.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }
    if (!relationshipsPost.equals(other.relationshipsPost)) {
      if (relationshipsPost.size() < other.relationshipsPost.size()) {
        return -1;
      } else if (relationshipsPost.size() > other.relationshipsPost.size()) {
        return 1;
      } else {
        final Iterator<Relationship> thisIt = relationshipsPost.iterator();
        final Iterator<Relationship> otherIt = other.relationshipsPost.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("MongoCuration(");
    sb.append(attributesPre);
    sb.append(",");
    sb.append(attributesPost);
    sb.append(",");
    sb.append(externalPre);
    sb.append(",");
    sb.append(externaPost);
    sb.append(",");
    sb.append(relationshipsPre);
    sb.append(",");
    sb.append(relationshipsPost);
    sb.append(")");
    return sb.toString();
  }

  @JsonCreator
  public static MongoCuration build(
      @JsonProperty("attributesPre") final Collection<Attribute> attributesPre,
      @JsonProperty("attributesPost") final Collection<Attribute> attributesPost,
      @JsonProperty("externalReferencesPre") final Collection<ExternalReference> externalPre,
      @JsonProperty("externalReferencesPost") final Collection<ExternalReference> externaPost,
      @JsonProperty("relationshipsPre") final Collection<Relationship> relationshipsPre,
      @JsonProperty("relationshipsPost") final Collection<Relationship> relationshipsPost) {

    SortedSet<Attribute> sortedPreAttributes = new TreeSet<>();
    SortedSet<Attribute> sortedPostAttributes = new TreeSet<>();
    SortedSet<ExternalReference> sortedPreExternal = new TreeSet<>();
    SortedSet<ExternalReference> sortedPostExternal = new TreeSet<>();
    SortedSet<Relationship> sortedPreRelationships = new TreeSet<>();
    SortedSet<Relationship> sortedPostRelationships = new TreeSet<>();

    if (attributesPre != null) {
      sortedPreAttributes.addAll(attributesPre);
    }
    if (attributesPost != null) {
      sortedPostAttributes.addAll(attributesPost);
    }
    if (externalPre != null) {
      sortedPreExternal.addAll(externalPre);
    }
    if (externaPost != null) {
      sortedPostExternal.addAll(externaPost);
    }
    if (relationshipsPre != null) {
      sortedPreRelationships.addAll(relationshipsPre);
    }
    if (relationshipsPost != null) {
      sortedPostRelationships.addAll(relationshipsPost);
    }

    sortedPreAttributes = Collections.unmodifiableSortedSet(sortedPreAttributes);
    sortedPostAttributes = Collections.unmodifiableSortedSet(sortedPostAttributes);
    sortedPreExternal = Collections.unmodifiableSortedSet(sortedPreExternal);
    sortedPostExternal = Collections.unmodifiableSortedSet(sortedPostExternal);
    sortedPreRelationships = Collections.unmodifiableSortedSet(sortedPreRelationships);
    sortedPostRelationships = Collections.unmodifiableSortedSet(sortedPostRelationships);

    final Hasher hasher = Hashing.sha256().newHasher();
    for (final Attribute a : sortedPreAttributes) {
      hasher.putUnencodedChars(a.getType());
      hasher.putUnencodedChars(a.getValue());
      if (a.getUnit() != null) {
        hasher.putUnencodedChars(a.getUnit());
      }
      if (a.getIri() != null) {
        for (final String iri : a.getIri()) {
          hasher.putUnencodedChars(iri);
        }
      }
    }
    for (final Attribute a : sortedPostAttributes) {
      hasher.putUnencodedChars(a.getType());
      hasher.putUnencodedChars(a.getValue());
      if (a.getUnit() != null) {
        hasher.putUnencodedChars(a.getUnit());
      }
      if (a.getIri() != null) {
        for (final String iri : a.getIri()) {
          hasher.putUnencodedChars(iri);
        }
      }
    }
    for (final ExternalReference a : sortedPreExternal) {
      hasher.putUnencodedChars(a.getUrl());
      if (a.getDuo() != null) {
        for (final String duo : a.getDuo()) {
          hasher.putUnencodedChars(duo);
        }
      }
    }
    for (final ExternalReference a : sortedPostExternal) {
      hasher.putUnencodedChars(a.getUrl());
      if (a.getDuo() != null) {
        for (final String duo : a.getDuo()) {
          hasher.putUnencodedChars(duo);
        }
      }
    }
    for (final Relationship a : sortedPreRelationships) {
      hasher.putUnencodedChars(a.getSource());
      hasher.putUnencodedChars(a.getTarget());
      hasher.putUnencodedChars(a.getType());
    }
    for (final Relationship a : sortedPostRelationships) {
      hasher.putUnencodedChars(a.getSource());
      hasher.putUnencodedChars(a.getTarget());
      hasher.putUnencodedChars(a.getType());
    }
    final String hash = hasher.hash().toString();

    return new MongoCuration(
        sortedPreAttributes,
        sortedPostAttributes,
        sortedPreExternal,
        sortedPostExternal,
        sortedPreRelationships,
        sortedPostRelationships,
        hash);
  }
}
