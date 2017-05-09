package uk.ac.ebi.biosamples.neo.service.modelconverter;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceLink;
import uk.ac.ebi.biosamples.neo.model.NeoSample;

@Service
public class ExternalReferenceLinkToNeoExternalReferenceLinkConverter
		implements Converter<ExternalReferenceLink, NeoExternalReferenceLink> {

	@Override
	public NeoExternalReferenceLink convert(ExternalReferenceLink erl) {
		if (erl == null) return null;
		
		return NeoExternalReferenceLink.build(
				NeoSample.create(erl.getSample()), 
				NeoExternalReference.build(erl.getUrl()));
	}

}
