package uk.ac.ebi.biosamples.solr.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.springframework.web.bind.annotation.ModelAttribute;

import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

public class SampleFacets implements Iterable<SampleFacet> {
/*
	//map of solr field (e.g. Organism_av_ss) to value (e.g. Homo sapiens) to count (e.g. 3232) 
	private SortedMap<String, SortedMap<String, Long>> facets;
	
	private SortedMap<String, Long> facetTotals;
	
	private SampleFacets() {

	}
	
	@ModelAttribute("fields")
	public List<String> getFields() {
		List<String> fields = new ArrayList<>(facets.keySet());
		Collections.reverse(fields);
		return Collections.unmodifiableList(fields);
	}

	@ModelAttribute("attributetypes")
	public List<String> getAttributeTypes() {
		List<String> attributeTypes = new ArrayList<>();
		for (String fieldName : facets.keySet()) {
			attributeTypes.add(fieldToAttributeType(fieldName));
		}
		Collections.reverse(attributeTypes);
		return Collections.unmodifiableList(attributeTypes);
	}
	
	public long getFieldTotal(String field) {
		return facetTotals.get(field);
	}
	
	public List<String> getValues(String field) {
		List<String> values = new ArrayList<>(facets.get(field).keySet());
		Collections.reverse(values);
		return Collections.unmodifiableList(values);
	}
	
	public long getValueCount(String field, String value) {
		return facets.get(field).get(value);
	}
	
		
	public static SampleFacets build(SortedMap<String, SortedMap<String, Long>> facets, SortedMap<String, Long> facetTotals) {
		SampleFacets sampleFacets = new SampleFacets();
		sampleFacets.facets = facets;
		sampleFacets.facetTotals = facetTotals;
		return sampleFacets;
	}	
	*/
	
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
			//cleanup if needed
			facet = SolrSampleService.fieldToAttributeType(facet);
			
			facets.add(new SampleFacet(facet, count));
			
			//sort it into a descending order
			Collections.sort(facets);
			Collections.reverse(facets);
			
			return this;
		}

		public SampleFacetsBuilder addFacetValue(String facet, String value, long count) {
			//cleanup if needed		
			facet = SolrSampleService.fieldToAttributeType(facet);
			
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
