package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
		log.trace("Applying curation "+curationLink+" to sample "+sample.getAccession());
		Curation curation = curationLink.getCuration();
		
		SortedSet<Attribute> attributes = new TreeSet<Attribute>(sample.getAttributes());
		SortedSet<ExternalReference> externalReferences = new TreeSet<ExternalReference>(sample.getExternalReferences());
		//remove pre-curation things
		for (Attribute attribute : curation.getAttributesPre()) {
			if (!attributes.contains(attribute)) {
				throw new IllegalArgumentException("Attempting to apply curation "+curation+" to sample "+sample);
			}
			attributes.remove(attribute);
		}
		for (ExternalReference externalReference : curation.getExternalReferencesPre()) {
			if (!externalReferences.contains(externalReference)) {
				throw new IllegalArgumentException("Attempting to apply curation "+curation+" to sample "+sample);
			}
			externalReferences.remove(externalReference);
		}
		//add post-curation things
		for (Attribute attribute : curation.getAttributesPost()) {
			if (attributes.contains(attribute)) {
				throw new IllegalArgumentException("Attempting to apply curation "+curation+" to sample "+sample);
			}
			attributes.add(attribute);
		}
		for (ExternalReference externalReference : curation.getExternalReferencesPost()) {
			if (externalReferences.contains(externalReference)) {
				throw new IllegalArgumentException("Attempting to apply curation "+curation+" to sample "+sample);
			}
			externalReferences.add(externalReference);
		}
		
		//update the sample's update date
		Instant update = sample.getUpdate();
		if (curationLink.getCreated().isAfter(update)) {
			update = curationLink.getCreated();
		}
		
//		return Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(),
//				sample.getRelease(), update, attributes, sample.getRelationships(), externalReferences,
//				sample.getOrganizations(), sample.getContacts(), sample.getPublications());
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
		
		Set<CurationLink> curationLinks = new HashSet<>();
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

		//filter curation links to remove conflicts
		curationLinks = filterConflictingCurationLinks(curationLinks);

		boolean curationApplied = true;
		while (curationApplied && curationLinks.size() > 0) {
			Iterator<CurationLink> it = curationLinks.iterator();
			curationApplied = false;
			while (it.hasNext()) {
				CurationLink curationLink = it.next();
				try {
					sample = applyCurationLinkToSample(sample, curationLink);
					it.remove();
					curationApplied = true;
				} catch (IllegalArgumentException e) {
					//do nothing, will try again next loop
				}
			}
		}
		if (!curationApplied) {
			//we stopped because we didn't apply any curation
			//therefore we have some curations that can't be applied
			//this is a warning
			log.warn("Unapplied curation on "+sample.getAccession());
		}
		return sample;
	}
	
	private Set<CurationLink> filterConflictingCurationLinks(Set<CurationLink> curationLinks) {
		Set<CurationLink> filteredCurationLinks = new HashSet<>();
		for (CurationLink curationLink : curationLinks) {
			boolean conflicts = false;
			for (CurationLink otherCurationLink : curationLinks) {
				if (otherCurationLink.equals(curationLink)) {
					continue;
				}
				
				log.trace("Comparing  "+curationLink.getCuration().getAttributesPre()+" with "+otherCurationLink.getCuration().getAttributesPre());
				
				Set<Attribute> intersection = new HashSet<>(curationLink.getCuration().getAttributesPre());
				intersection.retainAll(otherCurationLink.getCuration().getAttributesPre());
				if (intersection.size() > 0) {
					conflicts = true;
					break;
				}
			}
			if (!conflicts) {
				log.trace("Adding curationLink "+curationLink);
				filteredCurationLinks.add(curationLink);
			}
		}
		return filteredCurationLinks;
	}

}
