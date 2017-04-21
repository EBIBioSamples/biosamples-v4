package uk.ac.ebi.biosamples.neo.service.modelconverter;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoCurationApplication;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceApplication;
import uk.ac.ebi.biosamples.neo.model.NeoSample;

@Service
@ConfigurationPropertiesBinding
public class NeoCurationToCurationConverter
		implements Converter<NeoCuration, Curation> {

	@Autowired
	private NeoAttributeToAttributeConverter neoAttributeToAttributeConverter;
	
	@Override
	public Curation convert(NeoCuration neo) {
		SortedSet<String> samples = new TreeSet<>();
		for (NeoCurationApplication application : neo.getApplications()) {
			samples.add(application.getTarget().getAccession());
		}
		
		Set<Attribute> preAttributes = new HashSet<>();
		Set<Attribute> postAttributes = new HashSet<>();
		
		return Curation.build(preAttributes, postAttributes, samples);
		
	}

}
