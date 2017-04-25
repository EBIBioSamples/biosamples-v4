package uk.ac.ebi.biosamples.neo.service.modelconverter;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoCurationApplication;

@Service
@ConfigurationPropertiesBinding
public class NeoCurationToCurationConverter
		implements Converter<NeoCuration, Curation> {

	@Override
	public Curation convert(NeoCuration neo) {
		SortedSet<String> samples = new TreeSet<>();
		for (NeoCurationApplication application : neo.getApplications()) {
			samples.add(application.getTarget().getAccession());
		}
		
		Set<Attribute> preAttributes = new HashSet<>();
		Set<Attribute> postAttributes = new HashSet<>();
		
		//TODO finish
		
		return Curation.build(preAttributes, postAttributes, samples);
		
	}

}
