package uk.ac.ebi.biosamples.solr.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.solr.model.SolrSample;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SampleToSolrSampleConverter implements Converter<Sample, SolrSample> {

	
	private final DateTimeFormatter solrDateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'");
	
	@Override
	public SolrSample convert(Sample sample) {
		Map<String, List<String>> attributeValues = null;
		Map<String, List<String>> attributeIris = null;
		Map<String, List<String>> attributeUnits = null;
		Map<String, List<String>> sourceRelationships = null;
		Map<String, List<String>> targetRelationships = null;

		if (sample.getCharacteristics() != null && sample.getCharacteristics().size() > 0) {
			attributeValues = new HashMap<>();
			attributeIris = new HashMap<>();
			attributeUnits = new HashMap<>();

			for (Attribute attr : sample.getCharacteristics()) {
				
				String key = attr.getType();
				//key = SolrSampleService.attributeTypeToField(key);
				
				String value = attr.getValue();
				//if its longer than 255 characters, don't add it to solr
				//solr cant index long things well, and its probably not useful for search
				if (value.length() > 255) {
					continue;
				}
				
				if (!attributeValues.containsKey(key)) {
					attributeValues.put(key, new ArrayList<>());
				}
				
				//if there is a unit, add it to the value for search & facet purposes
				if (attr.getUnit() != null) {
					value = value+" ("+attr.getUnit()+")";
				}
				attributeValues.get(key).add(value);

				if (!attributeIris.containsKey(key)) {
					attributeIris.put(key, new ArrayList<>());
				}
				if (attr.getIri() == null) {
					attributeIris.get(key).add("");
				} else {
					attributeIris.get(key).add(attr.getIri().toString());
				}

				if (!attributeUnits.containsKey(key)) {
					attributeUnits.put(key, new ArrayList<>());
				}
				if (attr.getUnit() == null) {
					attributeUnits.get(key).add("");
				} else {
					attributeUnits.get(key).add(attr.getUnit());
				}
			}
		}	
		//turn external reference into additional attributes for facet & filter
		for (ExternalReference externalReference : sample.getExternalReferences()) {
			String value = getNickname(externalReference.getUrl());
			String key = "external reference";
			
			if (attributeValues == null) {
				attributeValues = new HashMap<>();
			}			
			if (!attributeValues.containsKey(key)) {
				attributeValues.put(key, new ArrayList<>());
			}
			attributeValues.get(key).add(value);
		}

		// Add relationships owned by sample
		if ( sample.getSourceRelationships() != null && !sample.getSourceRelationships().isEmpty()) {
			sourceRelationships = new HashMap<>();
			SortedSet<Relationship> sourceRels = sample.getSourceRelationships();
			for (Relationship rel : sourceRels) {
				sourceRelationships.computeIfAbsent(rel.getType(), type -> new ArrayList<>()).add(rel.getTarget());
			}
		}

		// Add relationships for which sample is the target
		if ( sample.getTargetRelationships() != null && !sample.getTargetRelationships().isEmpty()) {
			targetRelationships = new HashMap<>();
			SortedSet<Relationship> targetRels = sample.getTargetRelationships();
			for (Relationship rel: targetRels) {
				targetRelationships.computeIfAbsent(rel.getType(), type -> new ArrayList<>()).add(rel.getSource());
            }
		}


		String releaseSolr = formatDate(sample.getRelease());
		String updateSolr = formatDate(sample.getUpdate());		

		
		return SolrSample.build(sample.getName(), sample.getAccession(), releaseSolr, updateSolr,
				attributeValues, attributeIris, attributeUnits, sourceRelationships, targetRelationships);
	}
	
	private String formatDate(LocalDateTime d) {
		//this ensures that all components are present, even if they default to zero
		int year = d.getYear();
		int month = d.getMonthValue();
		int dayOfMonth = d.getDayOfMonth();
		int hour = d.getHour();
		int minute = d.getMinute();
		int second = d.getSecond();
		int nano = d.getNano();	
		
		d = LocalDateTime.of(year,month,dayOfMonth,hour,minute,second,nano);		
		String solrDate =  solrDateTimeFormatter.format(d);
		return solrDate;
	}
	
	private String getNickname(String url) {
		//TODO make this more configurable
		if (url.contains("www.ebi.ac.uk/ena")) {
			return "ENA";
		} else if (url.contains("www.ebi.ac.uk/arrayexpress")) {
			return "ArrayExpress";
		} else {
			return "other";
		}
	}
}
