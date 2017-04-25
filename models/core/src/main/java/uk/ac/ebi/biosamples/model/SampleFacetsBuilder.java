package uk.ac.ebi.biosamples.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SampleFacetsBuilder {
	
	private List<SampleFacet> facets = new LinkedList<>();
	
	public SampleFacetsBuilder addFacet(String facet, long count) {
		
		facets.add(SampleFacet.build(facet, new LinkedList<>(), count));
		
		//sort it into a descending order
		Collections.sort(facets);
		Collections.reverse(facets);
		
		return this;
	}

	public SampleFacetsBuilder addFacetValue(String facet, String value, long count) {
		
		for (int i = 0; i < facets.size(); i++) {
			SampleFacet sampleFacet = facets.get(i);
			if (sampleFacet.getLabel().equals(facet)) {
				//found an existing facet that matches
				List<SampleFacetValue> facetValues = new LinkedList<>(sampleFacet.getValues()); 
				facetValues.add(new SampleFacetValue(value, count));

				//sort it into a descending order
				Collections.sort(facetValues);
				Collections.reverse(facetValues);
				
				//replace the previous facet in the list
				facets.set(i, SampleFacet.build(sampleFacet.getLabel(), facetValues, 
						sampleFacet.getCount()));
				return this;
			}
		}
		throw new IllegalArgumentException("Unable to find facet "+facet);	
	}
	
	public List<SampleFacet> build() {
		return Collections.unmodifiableList(new LinkedList<>(facets));
	}
}