package uk.ac.ebi.biosamples.solr.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.models.Attribute;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.solr.model.SolrSample;

@Service
public class SolrSampleToSampleConverter implements Converter<SolrSample, Sample> {

	private final DateTimeFormatter solrDateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'");

	@Override
	public Sample convert(SolrSample sample) {

		SortedSet<Attribute> attributes = null;
		for (String type : sample.getAttributeValues().keySet()) {
			for (int i = 0; i < sample.getAttributeValues().get(type).size(); i++) {
				if (attributes == null) {
					attributes = new TreeSet<>();
				}
				String key = type.substring(0, type.length() - 6);
				String value = sample.getAttributeValues().get(type).get(i);
				String iri = null;
				String unit = null;
				Attribute attr = Attribute.build(key, value, iri, unit);
			}
		}

		LocalDateTime release = LocalDateTime.parse(sample.getRelease(), solrDateTimeFormatter);
		LocalDateTime update = LocalDateTime.parse(sample.getUpdate(), solrDateTimeFormatter);
		
		return Sample.build(sample.getName(), sample.getAccession(), release, update,
				attributes, null);
	}
}
