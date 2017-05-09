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


public class Curation implements Comparable<Curation>{
	
	private SortedSet<Attribute> attributesPre;

	private SortedSet<Attribute> attributesPost;
	
	private String hash;
	
	private Curation(Collection<Attribute> preAttributes, 
			Collection<Attribute> postAttributes,
			String hash) {
		this.attributesPre = Collections.unmodifiableSortedSet(new TreeSet<>(preAttributes));
		this.attributesPost = Collections.unmodifiableSortedSet(new TreeSet<>(postAttributes));
		this.hash = hash;
	}
	
	public SortedSet<Attribute> getAttributesPre() {
		return attributesPre;
	}
	
	public SortedSet<Attribute> getAttributesPost() {
		return attributesPost;
	}
	
	public String getHash() {
		return hash;
	}
	
    @Override
    public int hashCode() {
    	return Objects.hash(attributesPre, attributesPost);
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
		return 0;
	}

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("Curation(");
    	sb.append(attributesPre);
    	sb.append(",");
    	sb.append(attributesPost);
    	sb.append(")");
    	return sb.toString();
    }

    @JsonCreator
    public static Curation build(@JsonProperty("pre") Collection<Attribute> attributesPre, 
			@JsonProperty("post") Collection<Attribute> attributesPost) {
    	
		SortedSet<Attribute> sortedPreAttributes = new TreeSet<>();
		SortedSet<Attribute> sortedPostAttributes = new TreeSet<>();
		
		if (attributesPre != null) sortedPreAttributes.addAll(attributesPre);
		if (attributesPost != null) sortedPostAttributes.addAll(attributesPost);

		sortedPreAttributes = Collections.unmodifiableSortedSet(sortedPreAttributes);
		sortedPostAttributes = Collections.unmodifiableSortedSet(sortedPostAttributes);
		

    	Hasher hasher = Hashing.sha256().newHasher();
    	for (Attribute a : sortedPreAttributes) {
    		hasher.putUnencodedChars(a.getType());
    		hasher.putUnencodedChars(a.getValue());
    		if (a.getUnit() != null) {
    			hasher.putUnencodedChars(a.getUnit());
    		}
    		if (a.getIri() != null) {
    			hasher.putUnencodedChars(a.getIri());
    		}
    	}
    	for (Attribute a : sortedPostAttributes) {
    		hasher.putUnencodedChars(a.getType());
    		hasher.putUnencodedChars(a.getValue());
    		if (a.getUnit() != null) {
    			hasher.putUnencodedChars(a.getUnit());
    		}
    		if (a.getIri() != null) {
    			hasher.putUnencodedChars(a.getIri());
    		}
    	}
    	String hash = hasher.hash().toString();
		
		return new Curation(sortedPreAttributes, sortedPostAttributes, hash);
	}
}

