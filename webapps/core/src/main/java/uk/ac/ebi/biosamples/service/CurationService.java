package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoCurationLink;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationLinkRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.CurationLinkToNeoCurationLinkConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoCurationLinkToCurationLinkConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoCurationToCurationConverter;

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
	@Autowired
	private CurationLinkToNeoCurationLinkConverter curationLinkToNeoCurationLinkConverter;

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

	public Page<CurationLink> getCurationLinksForSample(String accession, Pageable pageable) {
		Page<NeoCurationLink> pageNeoCurationLink = neoCurationLinkRepository.findBySampleAccession(accession, pageable);		
		//get them in greater depth
		pageNeoCurationLink = pageNeoCurationLink.map(nxr -> neoCurationLinkRepository.findOneByHash(nxr.getHash(), 2));		
		//convert them into a state to return
		Page<CurationLink> pageCuration = pageNeoCurationLink.map(neoCurationLinkToCurationLinkConverter);		
		return pageCuration;
	}

	public CurationLink getCurationLink(String hash) {
		NeoCurationLink neo = neoCurationLinkRepository.findOneByHash(hash, 1);
		CurationLink link = neoCurationLinkToCurationLinkConverter.convert(neo);
		return link;
	}
	
	public CurationLink store(CurationLink curationLink) {
		NeoCurationLink neo = curationLinkToNeoCurationLinkConverter.convert(curationLink);
		neo = neoCurationLinkRepository.save(neo);
		return neoCurationLinkToCurationLinkConverter.convert(neo);
	}

}
