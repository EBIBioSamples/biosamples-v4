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
		Page<NeoExternalReferenceLink> neoPage = neoExternalReferenceLinkRepository.findAll(pageable,1);
		Page<ExternalReferenceLink> page = neoPage.map(neoExternalReferenceLinkToExternalReferenceLinkConverter);
		return page;
	}

	public ExternalReferenceLink getExternalReferenceLink(String id) {
		NeoExternalReferenceLink neo = neoExternalReferenceLinkRepository.findOneByHash(id, 1);
		ExternalReferenceLink link = neoExternalReferenceLinkToExternalReferenceLinkConverter.convert(neo);
		return link;
	}
	
	public Page<ExternalReferenceLink> getExternalReferenceLinksForSample(String accession, Pageable pageable) {
		Page<NeoExternalReferenceLink> neoPage = neoExternalReferenceLinkRepository.findBySampleAccession(accession, pageable);	
		//get them in greater depth
		neoPage.map(nxr -> neoExternalReferenceLinkRepository.findOne(nxr.getHash(), 3));		
		//convert them into a state to return
		Page<ExternalReferenceLink> page = neoPage.map(neoExternalReferenceLinkToExternalReferenceLinkConverter);
		return page;
	}

	public Page<ExternalReferenceLink> getExternalReferenceLinksForExternalReference(String urlHash, Pageable pageable) {
		Page<NeoExternalReferenceLink> neoPage = neoExternalReferenceLinkRepository.findByExternalReferenceUrlHash(urlHash, pageable);	
		//get them in greater depth
		neoPage.map(nxr -> neoExternalReferenceLinkRepository.findOne(nxr.getHash(), 3));		
		//convert them into a state to return
		Page<ExternalReferenceLink> page = neoPage.map(neoExternalReferenceLinkToExternalReferenceLinkConverter);
		return page;
	}
}
