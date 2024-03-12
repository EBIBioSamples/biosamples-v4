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
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.util.*;
import lombok.Data;

@Data
public class Curation implements Comparable<Curation> {
  @JsonProperty("attributesPre")
  private final SortedSet<Attribute> attributesPre;

  @JsonProperty("attributesPost")
  private final SortedSet<Attribute> attributesPost;

  private final SortedSet<ExternalReference> externalPre;
  private final SortedSet<ExternalReference> externalPost;

  @JsonProperty("relationshipsPre")
  private final SortedSet<Relationship> relationshipsPre;

  @JsonProperty("relationshipsPost")
  private final SortedSet<Relationship> relationshipsPost;

  private final String hash;

  private Curation(
      final Collection<Attribute> attributesPre,
      final Collection<Attribute> attributesPost,
      final Collection<ExternalReference> externalPre,
      final Collection<ExternalReference> externalPost,
      final Collection<Relationship> relationshipsPre,
      final Collection<Relationship> relationshipsPost,
      final String hash) {
    this.attributesPre = Collections.unmodifiableSortedSet(new TreeSet<>(attributesPre));
    this.attributesPost = Collections.unmodifiableSortedSet(new TreeSet<>(attributesPost));
    this.externalPre = Collections.unmodifiableSortedSet(new TreeSet<>(externalPre));
    this.externalPost = Collections.unmodifiableSortedSet(new TreeSet<>(externalPost));
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

  @JsonProperty("externalReferencesPre")
  public SortedSet<ExternalReference> getExternalReferencesPre() {
    return externalPre;
  }

  @JsonProperty("externalReferencesPost")
  public SortedSet<ExternalReference> getExternalReferencesPost() {
    return externalPost;
  }

  @Override
  public int compareTo(final Curation other) {
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
    if (!externalPost.equals(other.externalPost)) {
      if (externalPost.size() < other.externalPost.size()) {
        return -1;
      } else if (externalPost.size() > other.externalPost.size()) {
        return 1;
      } else {
        final Iterator<ExternalReference> thisIt = externalPost.iterator();
        final Iterator<ExternalReference> otherIt = other.externalPost.iterator();
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

  @JsonCreator
  public static Curation build(
      @JsonProperty("attributesPre") final Collection<Attribute> attributesPre,
      @JsonProperty("attributesPost") final Collection<Attribute> attributesPost,
      @JsonProperty("externalReferencesPre") final Collection<ExternalReference> externalPre,
      @JsonProperty("externalReferencesPost") final Collection<ExternalReference> externalPost,
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
    if (externalPost != null) {
      sortedPostExternal.addAll(externalPost);
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

      /*Consider tag if present*/
      if (a.getTag() != null) {
        hasher.putUnencodedChars(a.getTag());
      }
      if (a.getUnit() != null) {
        hasher.putUnencodedChars(a.getUnit());
      }
      for (final String iri : a.getIri()) {
        hasher.putUnencodedChars(iri);
      }
    }
    for (final Attribute a : sortedPostAttributes) {
      hasher.putUnencodedChars(a.getType());
      hasher.putUnencodedChars(a.getValue());
      /*Consider tag if present*/
      if (a.getTag() != null) {
        hasher.putUnencodedChars(a.getTag());
      }
      if (a.getUnit() != null) {
        hasher.putUnencodedChars(a.getUnit());
      }
      for (final String iri : a.getIri()) {
        hasher.putUnencodedChars(iri);
      }
    }
    for (final ExternalReference a : sortedPreExternal) {
      hasher.putUnencodedChars(a.getUrl());
      for (final String s : a.getDuo()) {
        hasher.putUnencodedChars(s);
      }
    }
    for (final ExternalReference a : sortedPostExternal) {
      hasher.putUnencodedChars(a.getUrl());
      for (final String s : a.getDuo()) {
        hasher.putUnencodedChars(s);
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

    return new Curation(
        sortedPreAttributes,
        sortedPostAttributes,
        sortedPreExternal,
        sortedPostExternal,
        sortedPreRelationships,
        sortedPostRelationships,
        hash);
  }

  //	@JsonCreator
  //	public static Curation build(@JsonProperty("attributesPre") Collection<Attribute>
  // attributesPre,
  //								 @JsonProperty("attributesPost") Collection<Attribute> attributesPost,
  //								 @JsonProperty("externalReferencesPre") Collection<ExternalReference> externalPre,
  //								 @JsonProperty("externalReferencesPost") Collection<ExternalReference> externalPost) {
  //
  //		return build(attributesPre, attributesPost, externalPre, externalPost, null, null);
  //	}

  public static Curation build(
      final Collection<Attribute> attributesPre,
      final Collection<Attribute> attributesPost,
      final Collection<ExternalReference> externalPre,
      final Collection<ExternalReference> externalPost) {

    return build(attributesPre, attributesPost, externalPre, externalPost, null, null);
  }

  public static Curation build(
      final Collection<Attribute> attributesPre, final Collection<Attribute> attributesPost) {
    return build(attributesPre, attributesPost, null, null);
  }

  public static Curation build(final Attribute attributePre, final Attribute attributePost) {

    if (attributePre == null && attributePost == null) {
      throw new IllegalArgumentException("Must specify pre and/or post attribute");
    } else if (attributePre == null) {
      // insertion curation
      return build(null, Collections.singleton(attributePost), null, null);
    } else if (attributePost == null) {
      // deletion curation
      return build(Collections.singleton(attributePre), null, null, null);
    } else {
      // one-to-one edit curation
      return build(
          Collections.singleton(attributePre), Collections.singleton(attributePost), null, null);
    }
  }
}
