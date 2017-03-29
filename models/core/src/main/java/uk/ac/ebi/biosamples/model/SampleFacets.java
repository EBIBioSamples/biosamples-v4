package uk.ac.ebi.biosamples.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SampleFacets implements Iterable<SampleFacet> {
	
	private List<SampleFacet> facets;

	private SampleFacets(List<SampleFacet> facets) {
		this.facets = facets;
	}
	
	@Override
	public Iterator<SampleFacet> iterator() {
		return facets.iterator();
	}
	
	public static class SampleFacetsBuilder {
		
		private List<SampleFacet> facets = new ArrayList<>();
		
		public SampleFacetsBuilder addFacet(String facet, long count) {
			
			facets.add(new SampleFacet(facet, count));
			
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
					List<SampleFacetValue> facetValues = new ArrayList<>(sampleFacet.getValues()); 
					facetValues.add(new SampleFacetValue(value, count));

					//sort it into a descending order
					Collections.sort(facetValues);
					Collections.reverse(facetValues);
					
					//replace the previous facet in the list
					facets.set(i, new SampleFacet(sampleFacet.getLabel(), facetValues, 
							sampleFacet.getCount()));
					return this;
				}
			}
			throw new IllegalArgumentException("Unable to find facet "+facet);	
		}
		
		public SampleFacets build() {
			return new SampleFacets(facets);
		}
	}
}
