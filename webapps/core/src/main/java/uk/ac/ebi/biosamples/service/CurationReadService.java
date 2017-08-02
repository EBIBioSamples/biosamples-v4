package uk.ac.ebi.biosamples.service;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoCuration;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationLinkRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationLinkToCurationLinkConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationToCurationConverter;

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
		MongoCurationLink neo = mongoCurationLinkRepository.findOne(hash);
		CurationLink link = mongoCurationLinkToCurationLinkConverter.convert(neo);
		return link;
	}
	
	public Sample applyCurationToSample(Sample sample, Curation curation) {
		log.info("Applying curation "+curation+" to sample "+sample);
		
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
		
		return Sample.build(sample.getName(), sample.getAccession(), sample.getRelease(), sample.getUpdate(), attributes, sample.getRelationships(), externalReferences);
	}
	
	public Sample applyAllCurationToSample(Sample sample) {

		Set<Curation> curations = new HashSet<>();
		int pageNo = 0;
		Page<Curation> page;
		do {
			Pageable pageable = new PageRequest(pageNo, 1000);
			page = getCurationsForSample(sample.getAccession(), pageable);
			for (Curation curation : page) {
				curations.add(curation);
			}
			pageNo += 1;
		} while(pageNo < page.getTotalPages());
		

		boolean curationApplied = true;
		while (curationApplied && curations.size() > 0) {
			Iterator<Curation> it = curations.iterator();
			curationApplied = false;
			while (it.hasNext()) {
				Curation curation = it.next();
				try {
					sample = applyCurationToSample(sample, curation);
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

}
