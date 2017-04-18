package uk.ac.ebi.biosamples.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SampleFacet implements Iterable<SampleFacetValue>, Comparable<SampleFacet> {
	private final String label;
	private final long count;
	private final List<SampleFacetValue> values;
	
	private SampleFacet(String label, List<SampleFacetValue> values, long count) {
		if (values == null) {
			values = new LinkedList<>();
		}
		this.label = label;
		this.values = Collections.unmodifiableList(new LinkedList<>(values));
		this.count = count;
	}
	
	public String getLabel() {
		return label;		
	}
	
	public long getCount() {
		return count;
	}
	
	public List<SampleFacetValue> getValues() {
		return Collections.unmodifiableList(values);
	}

	@Override
	public Iterator<SampleFacetValue> iterator() {
		return values.iterator();
	}

	@Override
	public int compareTo(SampleFacet o) {
		return Long.compare(this.count, o.count);
	}

	public int size() {
		return values.size();
	}
	
	
	@JsonCreator
	public static SampleFacet build(@JsonProperty("label") String label, 
			@JsonProperty("values") List<SampleFacetValue> values, @JsonProperty("count") long count) {
		return new SampleFacet(label,values,count);
	}
}