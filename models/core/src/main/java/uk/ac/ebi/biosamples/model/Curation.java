package uk.ac.ebi.biosamples.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Curation implements Comparable<Curation>{
	
	private SortedSet<Attribute> preAttributes;

	private SortedSet<Attribute> postAttributes;

	
	private Curation(SortedSet<Attribute> preAttributes, SortedSet<Attribute> postAttributes) {
	}
	
    @Override
    public int hashCode() {
    	return Objects.hash(preAttributes, postAttributes);
    }

	@Override
	public int compareTo(Curation other) {
		if (other == null) {
			return 1;
		}
		
		if (!this.preAttributes.equals(other.preAttributes)) {
			if (this.preAttributes.size() < other.preAttributes.size()) {
				return -1;
			} else if (this.preAttributes.size() > other.preAttributes.size()) {
				return 1;
			} else {
				Iterator<Attribute> thisIt = this.preAttributes.iterator();
				Iterator<Attribute> otherIt = other.preAttributes.iterator();
				while (thisIt.hasNext() && otherIt.hasNext()) {
					int val = thisIt.next().compareTo(otherIt.next());
					if (val != 0) return val;
				}
			}
		}
		if (!this.postAttributes.equals(other.postAttributes)) {
			if (this.postAttributes.size() < other.postAttributes.size()) {
				return -1;
			} else if (this.postAttributes.size() > other.postAttributes.size()) {
				return 1;
			} else {
				Iterator<Attribute> thisIt = this.postAttributes.iterator();
				Iterator<Attribute> otherIt = other.postAttributes.iterator();
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
    	sb.append(preAttributes);
    	sb.append(",");
    	sb.append(postAttributes);
    	sb.append(")");
    	return sb.toString();
    }

    @JsonCreator
	static public Curation build(@JsonProperty("pre") Collection<Attribute> preAttributes, @JsonProperty("post") Collection<Attribute> postAttributes) {
		SortedSet<Attribute> sortedPreAttributes = new TreeSet<>();
		SortedSet<Attribute> sortedPostAttributes = new TreeSet<>();
		if (preAttributes != null) sortedPreAttributes.addAll(preAttributes);
		if (postAttributes != null) sortedPostAttributes.addAll(postAttributes);

		sortedPreAttributes = Collections.unmodifiableSortedSet(sortedPreAttributes);
		sortedPostAttributes = Collections.unmodifiableSortedSet(sortedPostAttributes);
		
		return new Curation(sortedPreAttributes, sortedPostAttributes);
	}
}

