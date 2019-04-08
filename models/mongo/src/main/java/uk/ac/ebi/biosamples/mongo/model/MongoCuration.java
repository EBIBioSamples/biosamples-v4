package uk.ac.ebi.biosamples.mongo.model;

import java.util.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;

@Document
public class MongoCuration implements Comparable<MongoCuration>{
	
	private final SortedSet<Attribute> attributesPre;
	private final SortedSet<Attribute> attributesPost;

	private final SortedSet<ExternalReference> externalPre;
	private final SortedSet<ExternalReference> externaPost;
	
	@Id
	private String hash;
	
	private MongoCuration(Collection<Attribute> attributesPre, 
			Collection<Attribute> attributesPost,
			Collection<ExternalReference> externalPre, 
			Collection<ExternalReference> externaPost,
			String hash) {
		this.attributesPre = Collections.unmodifiableSortedSet(new TreeSet<>(attributesPre));
		this.attributesPost = Collections.unmodifiableSortedSet(new TreeSet<>(attributesPost));
		this.externalPre = Collections.unmodifiableSortedSet(new TreeSet<>(externalPre));
		this.externaPost = Collections.unmodifiableSortedSet(new TreeSet<>(externaPost));
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
        if (!(o instanceof MongoCuration)) {
            return false;
        }
        MongoCuration other = (MongoCuration) o;
        return Objects.equals(this.hash, other.hash)
        		&& Objects.equals(this.attributesPre, other.attributesPre)
        		&& Objects.equals(this.attributesPost, other.attributesPost)
        		&& Objects.equals(this.externalPre, other.externalPre)
        		&& Objects.equals(this.externaPost, other.externaPost);
    }

	@Override
	public int compareTo(MongoCuration other) {
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
		if (!this.externaPost.equals(other.externaPost)) {
			if (this.externaPost.size() < other.externaPost.size()) {
				return -1;
			} else if (this.externaPost.size() > other.externaPost.size()) {
				return 1;
			} else {
				Iterator<ExternalReference> thisIt = this.externaPost.iterator();
				Iterator<ExternalReference> otherIt = other.externaPost.iterator();
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
    	sb.append("MongoCuration(");
    	sb.append(attributesPre);
    	sb.append(",");
    	sb.append(attributesPost);
    	sb.append(",");
    	sb.append(externalPre);
    	sb.append(",");
    	sb.append(externaPost);
    	sb.append(")");
    	return sb.toString();
    }

    @JsonCreator
    public static MongoCuration build(@JsonProperty("attributesPre") Collection<Attribute> attributesPre, 
			@JsonProperty("attributesPost") Collection<Attribute> attributesPost,
			@JsonProperty("externalReferencesPre") Collection<ExternalReference> externalPre, 
			@JsonProperty("externalReferencesPost") Collection<ExternalReference> externaPost) {
    	
		SortedSet<Attribute> sortedPreAttributes = new TreeSet<>();
		SortedSet<Attribute> sortedPostAttributes = new TreeSet<>();    	
		SortedSet<ExternalReference> sortedPreExternal = new TreeSet<>();
		SortedSet<ExternalReference> sortedPostExternal = new TreeSet<>();
		
		if (attributesPre != null) sortedPreAttributes.addAll(attributesPre);
		if (attributesPost != null) sortedPostAttributes.addAll(attributesPost);		
		if (externalPre != null) sortedPreExternal.addAll(externalPre);
		if (externaPost != null) sortedPostExternal.addAll(externaPost);

		sortedPreAttributes = Collections.unmodifiableSortedSet(sortedPreAttributes);
		sortedPostAttributes = Collections.unmodifiableSortedSet(sortedPostAttributes);
		sortedPreExternal = Collections.unmodifiableSortedSet(sortedPreExternal);
		sortedPostExternal = Collections.unmodifiableSortedSet(sortedPostExternal);

    	Hasher hasher = Hashing.sha256().newHasher();
    	for (Attribute a : sortedPreAttributes) {
    		hasher.putUnencodedChars(a.getType());
    		hasher.putUnencodedChars(a.getValue());
    		if (a.getUnit() != null) {
    			hasher.putUnencodedChars(a.getUnit());
    		}
    		if (a.getIri() != null) {
    			for (String iri : a.getIri()) {
    				hasher.putUnencodedChars(iri);
    			}
    		}
    	}
    	for (Attribute a : sortedPostAttributes) {
    		hasher.putUnencodedChars(a.getType());
    		hasher.putUnencodedChars(a.getValue());
    		if (a.getUnit() != null) {
    			hasher.putUnencodedChars(a.getUnit());
    		}
    		if (a.getIri() != null) {
    			for (String iri : a.getIri()) {
    				hasher.putUnencodedChars(iri);
    			}
    		}
    	}
    	for (ExternalReference a : sortedPreExternal) {
    		hasher.putUnencodedChars(a.getUrl());
    		if (a.getDuo() != null) {
    			for (String duo : a.getDuo()) {
    				hasher.putUnencodedChars(duo);
				}
			}
    	}
    	for (ExternalReference a : sortedPostExternal) {
    		hasher.putUnencodedChars(a.getUrl());
			if (a.getDuo() != null) {
				for (String duo : a.getDuo()) {
					hasher.putUnencodedChars(duo);
				}
			}
    	}
    	String hash = hasher.hash().toString();
		
		return new MongoCuration(sortedPreAttributes, sortedPostAttributes, sortedPreExternal, sortedPostExternal, hash);
	}
}

