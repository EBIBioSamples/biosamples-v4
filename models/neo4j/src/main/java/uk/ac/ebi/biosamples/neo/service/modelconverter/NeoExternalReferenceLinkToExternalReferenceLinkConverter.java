package uk.ac.ebi.biosamples.neo.service.modelconverter;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceLink;
import uk.ac.ebi.biosamples.neo.model.NeoSample;

@Service
@ConfigurationPropertiesBinding
public class NeoExternalReferenceLinkToExternalReferenceLinkConverter
		implements Converter<NeoExternalReferenceLink, ExternalReferenceLink> {

	@Override
	public ExternalReferenceLink convert(NeoExternalReferenceLink neo) {
		if (neo == null) return null;
		//if there are errors here, neo probably wans't loaded with enough depth
		return ExternalReferenceLink.build( neo.getSample().getAccession(), neo.getExternalReference().getUrl(), neo.getHash());
		
	}

}
