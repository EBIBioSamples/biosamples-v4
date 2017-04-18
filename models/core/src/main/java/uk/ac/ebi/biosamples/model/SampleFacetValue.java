package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SampleFacetValue implements Comparable<SampleFacetValue>{
	public final String label;
	public final long count;
	
	public SampleFacetValue(String label, long count) {
		this.label = label;
		this.count = count;
	}

	@Override
	public int compareTo(SampleFacetValue o) {
		return Long.compare(this.count, o.count);
	}

	@JsonCreator
	public static SampleFacetValue build(@JsonProperty("label") String label, @JsonProperty("count") long count) {
		if (label == null || label.trim().length() == 0) {
			throw new IllegalArgumentException("label must not be blank");
		}			
		return new SampleFacetValue(label.trim(), count);
	}
}