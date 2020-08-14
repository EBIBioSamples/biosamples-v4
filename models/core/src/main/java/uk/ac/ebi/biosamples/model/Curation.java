package uk.ac.ebi.biosamples.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;


public class Curation implements Comparable<Curation> {
	
	private final SortedSet<Attribute> attributesPre;
	private final SortedSet<Attribute> attributesPost;

	private final SortedSet<ExternalReference> externalPre;
	private final SortedSet<ExternalReference> externalPost;

	private final SortedSet<Relationship> relationshipsPre;
	private final SortedSet<Relationship> relationshipsPost;

	private String hash;

	private Curation(Collection<Attribute> attributesPre,
			Collection<Attribute> attributesPost,
			Collection<ExternalReference> externalPre,
			Collection<ExternalReference> externalPost,
			Collection<Relationship> relationshipsPre,
			Collection<Relationship> relationshipsPost,
			String hash) {
		this.attributesPre = Collections.unmodifiableSortedSet(new TreeSet<>(attributesPre));
		this.attributesPost = Collections.unmodifiableSortedSet(new TreeSet<>(attributesPost));
		this.externalPre = Collections.unmodifiableSortedSet(new TreeSet<>(externalPre));
		this.externalPost = Collections.unmodifiableSortedSet(new TreeSet<>(externalPost));
		this.relationshipsPre = Collections.unmodifiableSortedSet(new TreeSet<>(relationshipsPre));
		this.relationshipsPost = Collections.unmodifiableSortedSet(new TreeSet<>(relationshipsPost));
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
		return externalPost;
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
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Curation)) {
            return false;
        }
        Curation other = (Curation) o;
        return Objects.equals(this.attributesPre, other.attributesPre) 
        		&& Objects.equals(this.attributesPost, other.attributesPost)
        		&& Objects.equals(this.externalPre, other.externalPre)
        		&& Objects.equals(this.externalPost, other.externalPost)
        		&& Objects.equals(this.relationshipsPre, other.relationshipsPre)
        		&& Objects.equals(this.relationshipsPost, other.relationshipsPost);
    }

	@Override
	public int compareTo(Curation other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.attributesPre.equals(other.attributesPre)) {
			if (this.attributesPre.size() < other.attributesPre.size()) {
				return -1;
			} else if (this.attributesPre.size() > other.attributesPre.size()) {
				return 1;
			} else {
				Iterator<Attribute> thisIt = this.attributesPre.iterator();
				Iterator<Attribute> otherIt = other.attributesPre.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		if (!this.attributesPost.equals(other.attributesPost)) {
			if (this.attributesPost.size() < other.attributesPost.size()) {
				return -1;
			} else if (this.attributesPost.size() > other.attributesPost.size()) {
				return 1;
			} else {
				Iterator<Attribute> thisIt = this.attributesPost.iterator();
				Iterator<Attribute> otherIt = other.attributesPost.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		
		if (!this.externalPre.equals(other.externalPre)) {
			if (this.externalPre.size() < other.externalPre.size()) {
				return -1;
			} else if (this.externalPre.size() > other.externalPre.size()) {
				return 1;
			} else {
				Iterator<ExternalReference> thisIt = this.externalPre.iterator();
				Iterator<ExternalReference> otherIt = other.externalPre.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		if (!this.externalPost.equals(other.externalPost)) {
			if (this.externalPost.size() < other.externalPost.size()) {
				return -1;
			} else if (this.externalPost.size() > other.externalPost.size()) {
				return 1;
			} else {
				Iterator<ExternalReference> thisIt = this.externalPost.iterator();
				Iterator<ExternalReference> otherIt = other.externalPost.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}

		if (!this.relationshipsPre.equals(other.relationshipsPre)) {
			if (this.relationshipsPre.size() < other.relationshipsPre.size()) {
				return -1;
			} else if (this.relationshipsPre.size() > other.relationshipsPre.size()) {
				return 1;
			} else {
				Iterator<Relationship> thisIt = this.relationshipsPre.iterator();
				Iterator<Relationship> otherIt = other.relationshipsPre.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		if (!this.relationshipsPost.equals(other.relationshipsPost)) {
			if (this.relationshipsPost.size() < other.relationshipsPost.size()) {
				return -1;
			} else if (this.relationshipsPost.size() > other.relationshipsPost.size()) {
				return 1;
			} else {
				Iterator<Relationship> thisIt = this.relationshipsPost.iterator();
				Iterator<Relationship> otherIt = other.relationshipsPost.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		return 0;
	}

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("Curation(");
    	sb.append(attributesPre);
    	sb.append(",");
    	sb.append(attributesPost);
    	sb.append(",");
    	sb.append(externalPre);
    	sb.append(",");
    	sb.append(externalPost);
    	sb.append(",");
    	sb.append(relationshipsPre);
    	sb.append(",");
    	sb.append(relationshipsPost);
    	sb.append(")");
    	return sb.toString();
    }

    @JsonCreator
    public static Curation build(@JsonProperty("attributesPre") Collection<Attribute> attributesPre,
			@JsonProperty("attributesPost") Collection<Attribute> attributesPost,
			@JsonProperty("externalReferencesPre") Collection<ExternalReference> externalPre,
			@JsonProperty("externalReferencesPost") Collection<ExternalReference> externalPost,
			@JsonProperty("relationshipsPre") Collection<Relationship> relationshipsPre,
			@JsonProperty("relationshipsPost") Collection<Relationship> relationshipsPost) {

		SortedSet<Attribute> sortedPreAttributes = new TreeSet<>();
		SortedSet<Attribute> sortedPostAttributes = new TreeSet<>();
		SortedSet<ExternalReference> sortedPreExternal = new TreeSet<>();
		SortedSet<ExternalReference> sortedPostExternal = new TreeSet<>();
		SortedSet<Relationship> sortedPreRelationships = new TreeSet<>();
		SortedSet<Relationship> sortedPostRelationships = new TreeSet<>();

		if (attributesPre != null) sortedPreAttributes.addAll(attributesPre);
		if (attributesPost != null) sortedPostAttributes.addAll(attributesPost);
		if (externalPre != null) sortedPreExternal.addAll(externalPre);
		if (externalPost != null) sortedPostExternal.addAll(externalPost);
		if (relationshipsPre != null) sortedPreRelationships.addAll(relationshipsPre);
		if (relationshipsPost != null) sortedPostRelationships.addAll(relationshipsPost);

		sortedPreAttributes = Collections.unmodifiableSortedSet(sortedPreAttributes);
		sortedPostAttributes = Collections.unmodifiableSortedSet(sortedPostAttributes);
		sortedPreExternal = Collections.unmodifiableSortedSet(sortedPreExternal);
		sortedPostExternal = Collections.unmodifiableSortedSet(sortedPostExternal);
		sortedPreRelationships = Collections.unmodifiableSortedSet(sortedPreRelationships);
		sortedPostRelationships = Collections.unmodifiableSortedSet(sortedPostRelationships);

    	Hasher hasher = Hashing.sha256().newHasher();
    	for (Attribute a : sortedPreAttributes) {
    		hasher.putUnencodedChars(a.getType());
    		hasher.putUnencodedChars(a.getValue());

    		/*Consider tag if present*/
    		if(a.getTag() != null) {
				hasher.putUnencodedChars(a.getTag());
			}
    		if (a.getUnit() != null) {
    			hasher.putUnencodedChars(a.getUnit());
    		}
    		for(String iri : a.getIri()) {
    			hasher.putUnencodedChars(iri);
    		}
    	}
    	for (Attribute a : sortedPostAttributes) {
    		hasher.putUnencodedChars(a.getType());
    		hasher.putUnencodedChars(a.getValue());
			/*Consider tag if present*/
			if(a.getTag() != null) {
				hasher.putUnencodedChars(a.getTag());
			}
    		if (a.getUnit() != null) {
    			hasher.putUnencodedChars(a.getUnit());
    		}
    		for(String iri : a.getIri()) {
    			hasher.putUnencodedChars(iri);
    		}
    	}
    	for (ExternalReference a : sortedPreExternal) {
    		hasher.putUnencodedChars(a.getUrl());
    		for (String s :a.getDuo()) {
    			hasher.putUnencodedChars(s);
			}
    	}
    	for (ExternalReference a : sortedPostExternal) {
    		hasher.putUnencodedChars(a.getUrl());
			for (String s :a.getDuo()) {
				hasher.putUnencodedChars(s);
			}
    	}
    	for (Relationship a : sortedPreRelationships) {
    		hasher.putUnencodedChars(a.getSource());
    		hasher.putUnencodedChars(a.getTarget());
    		hasher.putUnencodedChars(a.getType());
    	}
    	for (Relationship a : sortedPostRelationships) {
    		hasher.putUnencodedChars(a.getSource());
    		hasher.putUnencodedChars(a.getTarget());
    		hasher.putUnencodedChars(a.getType());
    	}
    	String hash = hasher.hash().toString();

		return new Curation(sortedPreAttributes, sortedPostAttributes, sortedPreExternal, sortedPostExternal, sortedPreRelationships, sortedPostRelationships, hash);
	}

//	@JsonCreator
//	public static Curation build(@JsonProperty("attributesPre") Collection<Attribute> attributesPre,
//								 @JsonProperty("attributesPost") Collection<Attribute> attributesPost,
//								 @JsonProperty("externalReferencesPre") Collection<ExternalReference> externalPre,
//								 @JsonProperty("externalReferencesPost") Collection<ExternalReference> externalPost) {
//
//		return build(attributesPre, attributesPost, externalPre, externalPost, null, null);
//	}

	public static Curation build(Collection<Attribute> attributesPre,
								 Collection<Attribute> attributesPost,
								 Collection<ExternalReference> externalPre,
								 Collection<ExternalReference> externalPost) {

		return build(attributesPre, attributesPost, externalPre, externalPost, null, null);
	}

    public static Curation build(Collection<Attribute> attributesPre, 
			Collection<Attribute> attributesPost) {
    	return build(attributesPre, attributesPost, null, null);
    }

    public static Curation build(Attribute attributePre, 
			Attribute attributePost) {
    	
    	if (attributePre == null && attributePost == null) {
    		throw new IllegalArgumentException("Must specify pre and/or post attribute");
    	} else if (attributePre == null) {
    		//insertion curation
    		return build(null, Collections.singleton(attributePost), null, null);
    	} else if (attributePost == null) {
    		//deletion curation
    		return build(Collections.singleton(attributePre), null, null, null);
    	} else {
    		//one-to-one edit curation
    		return build(Collections.singleton(attributePre), Collections.singleton(attributePost), null, null);
    	}
    }
    
}

