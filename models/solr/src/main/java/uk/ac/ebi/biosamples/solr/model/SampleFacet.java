package uk.ac.ebi.biosamples.solr.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SampleFacet implements Iterable<SampleFacetValue>, Comparable<SampleFacet> {
	private final String label;
	private List<SampleFacetValue> values;
	private final long count;
	
	public SampleFacet(String label, long count) {
		this.label = label;
		this.values = new ArrayList<>();
		this.count = count;
	}
	
	public SampleFacet(String label, List<SampleFacetValue> values, long count) {
		this.label = label;
		this.values = new ArrayList<>(values);
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
}