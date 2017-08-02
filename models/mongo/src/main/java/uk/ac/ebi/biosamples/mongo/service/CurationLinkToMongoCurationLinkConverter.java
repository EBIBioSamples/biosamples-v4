package uk.ac.ebi.biosamples.mongo.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;

@Service
public class CurationLinkToMongoCurationLinkConverter
		implements Converter<CurationLink, MongoCurationLink> {
	
	@Override
	public MongoCurationLink convert(CurationLink curationLink) {	
		return MongoCurationLink.build(curationLink.getSample(), curationLink.getCuration(), curationLink.getCreated());
	}

}
