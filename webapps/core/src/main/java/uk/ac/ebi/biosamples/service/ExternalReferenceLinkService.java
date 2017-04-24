package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceLink;
import uk.ac.ebi.biosamples.neo.repo.NeoExternalReferenceLinkRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoExternalReferenceLinkToExternalReferenceLinkConverter;

@Service
public class ExternalReferenceLinkService {

	@Autowired
	private NeoExternalReferenceLinkRepository neoExternalReferenceLinkRepository;
	
	@Autowired
	private NeoExternalReferenceLinkToExternalReferenceLinkConverter neoExternalReferenceLinkToExternalReferenceLinkConverter;

	public Page<ExternalReferenceLink> getPage(Pageable pageable) {
		Page<NeoExternalReferenceLink> neoPage = neoExternalReferenceLinkRepository.findAll(pageable,2);
		Page<ExternalReferenceLink> page = neoPage.map(neoExternalReferenceLinkToExternalReferenceLinkConverter);
		return page;
	}

	public ExternalReferenceLink getExternalReferenceLink(String id) {
		NeoExternalReferenceLink neo = neoExternalReferenceLinkRepository.findOne(id, 2);
		ExternalReferenceLink link = neoExternalReferenceLinkToExternalReferenceLinkConverter.convert(neo);
		return link;
	}

}
