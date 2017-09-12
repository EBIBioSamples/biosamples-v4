package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;

@Service
public class MongoCurationLinkToCurationLinkConverter
		implements Converter<MongoCurationLink, CurationLink> {
	
	@Override
	public CurationLink convert(MongoCurationLink mongoCurationLink) {	
		return CurationLink.build(mongoCurationLink.getSample(), mongoCurationLink.getCuration(), 
				mongoCurationLink.getDomain(), mongoCurationLink.getCreated());
	}

}
