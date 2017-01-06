package uk.ac.ebi.biosamples.solr.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.models.Attribute;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.solr.model.SolrSample;

@Service
public class SampleToSolrSampleConverter implements Converter<Sample, SolrSample> {

	private final DateTimeFormatter solrDateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'");
	
	@Override
	public SolrSample convert(Sample sample) {
		Map<String, List<String>> attributeValues = null;
		Map<String, List<String>> attributeIris = null;
		Map<String, List<String>> attributeUnits = null;
		
		if (sample.getAttributes() != null && sample.getAttributes().size() > 0) {
			attributeValues = new HashMap<>();
			attributeIris = new HashMap<>();
			attributeUnits = new HashMap<>();
			
			for (Attribute attr : sample.getAttributes()) {
				if (!attributeValues.containsKey(attr.getKey())) {
					attributeValues.put(attr.getKey(), new ArrayList<>());
				}
				String value = attr.getValue();
				//if its longer than 255 characters, don't add it to solr
				//solr cant index long things well, and its probably not useful for search
				if (value.length() > 255) {
					continue;
				}
				attributeValues.get(attr.getKey()).add(value);

				if (!attributeIris.containsKey(attr.getKey())) {
					attributeIris.put(attr.getKey(), new ArrayList<>());
				}
				if (attr.getIri() == null) {
					attributeIris.get(attr.getKey()).add("");
				} else {
					attributeIris.get(attr.getKey()).add(attr.getIri());
				}

				if (!attributeUnits.containsKey(attr.getKey())) {
					attributeUnits.put(attr.getKey(), new ArrayList<>());
				}
				if (attr.getUnit() == null) {
					attributeUnits.get(attr.getKey()).add("");
				} else {
					attributeUnits.get(attr.getKey()).add(attr.getUnit());
				}
			}
		}
		
		String releaseSolr =  solrDateTimeFormatter.format(sample.getRelease());
		String updateSolr = solrDateTimeFormatter.format(sample.getUpdate());
		
		return SolrSample.build(sample.getName(), sample.getAccession(), releaseSolr, updateSolr,
				attributeValues, attributeIris, attributeUnits);
	}
}
