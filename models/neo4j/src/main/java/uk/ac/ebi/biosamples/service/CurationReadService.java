package uk.ac.ebi.biosamples.service;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoCurationLink;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationLinkRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoCurationRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoCurationLinkToCurationLinkConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoCurationToCurationConverter;

@Service
public class CurationReadService {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private NeoCurationRepository neoCurationRepository;
	@Autowired
	private NeoCurationLinkRepository neoCurationLinkRepository;

	//TODO use a ConversionService to manage all these
	@Autowired
	private NeoCurationToCurationConverter neoCurationToCurationConverter;
	@Autowired
	private NeoCurationLinkToCurationLinkConverter neoCurationLinkToCurationLinkConverter;

	@Autowired
	private CurationApplicationService curationApplicationService;

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


	public Page<Curation> getCurationsForSample(String accession, Pageable pageable) {
		Page<NeoCuration> pageNeoCuration = neoCurationRepository.findBySampleAccession(accession, pageable,1);		
		//convert them into a state to return
		//Page<Curation> pageCuration = pageNeoCuration.map(neoCurationToCurationConverter);		
		

		//stream process each *in parallel*
		Page<Curation> pageCuration = new PageImpl<>(StreamSupport.stream(pageNeoCuration.spliterator(), true)
					.map(new Function<NeoCuration,Curation>() {
						@Override
						public Curation apply(NeoCuration nc) {
							return neoCurationToCurationConverter.convert(neoCurationRepository.findOneByHash(nc.getHash(), 1));
						}
					}).collect(Collectors.toList()), 
				pageable, pageNeoCuration.getTotalElements()); 
		
		
		return pageCuration;
	}

	public CurationLink getCurationLink(String hash) {
		NeoCurationLink neo = neoCurationLinkRepository.findOneByHash(hash, 1);
		CurationLink link = neoCurationLinkToCurationLinkConverter.convert(neo);
		return link;
	}
	
	public Sample getAndApplyCurationsToSample(Sample sample) {

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
		
		sample = curationApplicationService.applyAllCurationToSample(sample, curations);
		
		return sample;
	}

}
