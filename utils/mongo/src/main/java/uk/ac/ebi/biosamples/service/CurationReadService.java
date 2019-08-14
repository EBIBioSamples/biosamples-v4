package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.mongo.model.MongoCuration;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationLinkRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationLinkToCurationLinkConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationToCurationConverter;

import java.time.Instant;
import java.util.*;

@Service
public class CurationReadService {

	private Logger log = LoggerFactory.getLogger(getClass());
	@Autowired
	private MongoCurationRepository mongoCurationRepository;
	@Autowired
	private MongoCurationLinkRepository mongoCurationLinkRepository;

	//TODO use a ConversionService to manage all these
	@Autowired
	private MongoCurationLinkToCurationLinkConverter mongoCurationLinkToCurationLinkConverter;
	@Autowired
	private MongoCurationToCurationConverter mongoCurationToCurationConverter;
	
	public Page<Curation> getPage(Pageable pageable) {
		Page<MongoCuration> pageNeoCuration = mongoCurationRepository.findAll(pageable);
		Page<Curation> pageCuration = pageNeoCuration.map(mongoCurationToCurationConverter);		
		return pageCuration;
	}

	public Curation getCuration(String hash) {
		MongoCuration neoCuration = mongoCurationRepository.findOne(hash);
		if (neoCuration == null) {
			return null;
		} else {
			return mongoCurationToCurationConverter.convert(neoCuration);
		}
	}

	public Page<CurationLink> getCurationLinksForSample(String accession, Pageable pageable) {
		Page<MongoCurationLink> pageNeoCurationLink = mongoCurationLinkRepository.findBySample(accession, pageable);		
		//convert them into a state to return
		Page<CurationLink> pageCuration = pageNeoCurationLink.map(mongoCurationLinkToCurationLinkConverter);		
		return pageCuration;
	}


	public Page<Curation> getCurationsForSample(String accession, Pageable pageable) {
		return getCurationLinksForSample(accession, pageable).map(curationLink -> curationLink.getCuration());
	}

	public CurationLink getCurationLink(String hash) {
		MongoCurationLink mongoCurationLink = mongoCurationLinkRepository.findOne(hash);
		CurationLink link = mongoCurationLinkToCurationLinkConverter.convert(mongoCurationLink);
		return link;
	}
	
	/**
	 * This applies a given curation link to a sample and returns a new sample.
	 * 
	 * This needs a curation link rather than a curation object because the samples update date
	 * may be modified if the curation link is newer.
	 * 
	 * 
	 * @param sample
	 * @param curationLink
	 * @return
	 */
	public Sample applyCurationLinkToSample(Sample sample, CurationLink curationLink) {
		log.trace("Applying curation " + curationLink + " to sample " + sample.getAccession());
		Curation curation = curationLink.getCuration();
		
		SortedSet<Attribute> attributes = new TreeSet<>(sample.getAttributes());
		SortedSet<ExternalReference> externalReferences = new TreeSet<>(sample.getExternalReferences());
		//remove pre-curation things
		for (Attribute attribute : curation.getAttributesPre()) {
			if (!attributes.contains(attribute)) {
				throw new IllegalArgumentException("Failed to apply curation " + curation + " to sample " + sample);
			}
			attributes.remove(attribute);
		}
		for (ExternalReference externalReference : curation.getExternalReferencesPre()) {
			if (!externalReferences.contains(externalReference)) {
				throw new IllegalArgumentException("Failed to apply curation " + curation + " to sample " + sample);
			}
			externalReferences.remove(externalReference);
		}
		//add post-curation things
		for (Attribute attribute : curation.getAttributesPost()) {
			if (attributes.contains(attribute)) {
				throw new IllegalArgumentException("Failed to apply curation " + curation + " to sample " + sample);
			}
			attributes.add(attribute);
		}
		for (ExternalReference externalReference : curation.getExternalReferencesPost()) {
			if (externalReferences.contains(externalReference)) {
				throw new IllegalArgumentException("Failed to apply curation " + curation + " to sample " + sample);
			}
			externalReferences.add(externalReference);
		}
		
		//update the sample's update date
		Instant update = sample.getUpdate();
		if (curationLink.getCreated().isAfter(update)) {
			update = curationLink.getCreated();
		}

        return Sample.Builder.fromSample(sample)
				.withUpdate(update)
				.withAttributes(attributes)
				.withExternalReferences(externalReferences)
				.build();
	}
	
	public Sample applyAllCurationToSample(Sample sample, Optional<List<String>> curationDomains) {
        //short-circuit if no curation domains specified
		if (curationDomains.isPresent() && curationDomains.get().size()==0) {
			return sample;
		}

		//Try to apply curations in the order of creation date.
		//Because of the index in creation date mongo returns in that order
		Set<CurationLink> curationLinks = new LinkedHashSet<>();
		int pageNo = 0;
		Page<CurationLink> page;
		do {
			Pageable pageable = new PageRequest(pageNo, 1000);
			page = getCurationLinksForSample(sample.getAccession(), pageable);
			for (CurationLink curationLink : page) {
				if (curationDomains.isPresent()) {
					//curation domains restricted, curation must be part of that domain
					if (curationDomains.get().contains(curationLink.getDomain())) {
						curationLinks.add(curationLink);
					}
				} else {
					//no curation domain restriction, use all
					curationLinks.add(curationLink);
				}
			}
			pageNo += 1;
		} while(pageNo < page.getTotalPages());

		boolean failedCuration = false;
		for(CurationLink curation : curationLinks) {
			try {
				sample = applyCurationLinkToSample(sample, curation);
			} catch (IllegalArgumentException e) {
				failedCuration = true;
				log.trace(e.getMessage());
			}
		}

		if (failedCuration) {
			log.warn("Unapplied curation on sample: {}", sample.getAccession());
		}

		return sample;
	}

}
