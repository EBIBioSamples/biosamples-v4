package uk.ac.ebi.biosamples.model;

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
}