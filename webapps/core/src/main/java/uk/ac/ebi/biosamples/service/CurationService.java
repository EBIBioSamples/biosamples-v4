package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationLinkRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoExternalReferenceLinkRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoExternalReferenceRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoCurationLinkToCurationLinkConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoCurationToCurationConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoExternalReferenceLinkToExternalReferenceLinkConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoExternalReferenceToExternalReferenceConverter;

@Service
public class CurationService {

	@Autowired
	private NeoCurationRepository neoCurationRepository;
	@Autowired
	private NeoCurationLinkRepository neoCurationLinkRepository;
	
	@Autowired
	private NeoCurationToCurationConverter neoCurationToCurationConverter;
	@Autowired
	private NeoCurationLinkToCurationLinkConverter neoCurationLinkToCurationLinkConverter;

	public Page<Curation> getPage(Pageable pageable) {
		Page<NeoCuration> pageNeoCuration = neoCurationRepository.findAll(pageable,2);
		Page<Curation> pageCuration = pageNeoCuration.map(neoCurationToCurationConverter);		
		return pageCuration;
	}

	public Curation getCuration(String hash) {
		NeoCuration neoCuration = neoCurationRepository.findOneByHash(hash,2);
		if (neoCuration == null) {
			return null;
		} else {
			return neoCurationToCurationConverter.convert(neoCuration);
		}
	}

}
