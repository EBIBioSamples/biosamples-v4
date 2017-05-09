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
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoCurationLink;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;

@Service
public class CurationLinkToNeoCurationLinkConverter
		implements Converter<CurationLink, NeoCurationLink> {
	
	
	private final CurationToNeoCurationConverter curationToNeoCurationConverter;	
	private final NeoSampleRepository neoSampleRepository;

	
	public CurationLinkToNeoCurationLinkConverter(CurationToNeoCurationConverter neoCurationToCurationConverter,
			NeoSampleRepository neoSampleRepository) {
		this.curationToNeoCurationConverter = neoCurationToCurationConverter;
		this.neoSampleRepository = neoSampleRepository;
	}
	
	@Override
	public NeoCurationLink convert(CurationLink curationLink) {	
		if (curationLink == null) return null;
		
		NeoCuration neoCuration = curationToNeoCurationConverter.convert(curationLink.getCuration());	
		return NeoCurationLink.build(neoCuration, neoSampleRepository.findOneByAccession(curationLink.getSample(), 0));
	}

}
