package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LabelCountEntry implements Comparable<LabelCountEntry>{
	public final String label;
	public final long count;
	
	LabelCountEntry(String label, long count) {
		this.label = label;
		this.count = count;
	}

	@Override
	public int compareTo(LabelCountEntry o) {
		return Long.compare(this.count, o.count);
	}

	@JsonCreator
	public static LabelCountEntry build(@JsonProperty("label") String label, @JsonProperty("count") long count) {
		if (label == null || label.trim().length() == 0) {
			throw new IllegalArgumentException("label must not be blank");
		}			
		return new LabelCountEntry(label.trim(), count);
	}
}