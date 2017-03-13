package uk.ac.ebi.biosamples.solr.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.springframework.web.bind.annotation.ModelAttribute;

public class SampleFacets {

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
	
	public static String fieldToAttributeType(String field) {
		if (field.endsWith("_av_ss")) {
			return field.substring(0, field.length()-6);
		} else {
			throw new IllegalArgumentException("field "+field+" is not a valid field");
		}
	}
	
	public static String attributeTypeToField(String attributeType) {
		if (attributeType.endsWith("_av_ss")) {
			throw new IllegalArgumentException("AttributeType "+attributeType+" is not a valid attribute type");
		} else {
			return attributeType+"_av_ss";
		}
	}
}
