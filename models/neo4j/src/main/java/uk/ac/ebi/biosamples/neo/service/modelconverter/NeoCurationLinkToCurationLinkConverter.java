package uk.ac.ebi.biosamples.neo.service.modelconverter;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.neo.model.NeoCurationLink;

@Service
@ConfigurationPropertiesBinding
public class NeoCurationLinkToCurationLinkConverter
		implements Converter<NeoCurationLink, CurationLink> {
	
	
	private final NeoCurationToCurationConverter neoCurationToCurationConverter;

	
	public NeoCurationLinkToCurationLinkConverter(NeoCurationToCurationConverter neoCurationToCurationConverter) {
		this.neoCurationToCurationConverter = neoCurationToCurationConverter;
	}
	
	@Override
	public CurationLink convert(NeoCurationLink neo) {	
		Curation curation = neoCurationToCurationConverter.convert(neo.getCuration());	
		return CurationLink.build(neo.getSample().getAccession(), curation, neo.getCreated());
	}

}
