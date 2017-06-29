package uk.ac.ebi.biosamples.service;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import uk.ac.ebi.biosamples.WebappProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoSampleToSampleConverter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

/**
 * Service layer business logic for centralising repository access and
 * conversions between different controller. Use this instead of linking to
 * repositories directly.
 * 
 * @author faulcon
 *
 */
@Service
public class SamplePageService {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private NeoSampleRepository neoSampleRepository;
	
	
	//TODO use a ConversionService to manage all these
	@Autowired
	private NeoSampleToSampleConverter neoSampleToSampleConverter;

	@Autowired
	private SampleReadService sampleService;
	
	@Autowired
	private SolrSampleService solrSampleService;
	

	//@Cacheable(cacheNames=WebappProperties.getSamplesByText, sync=true)
	public Page<Sample> getSamplesByText(String text, MultiValueMap<String,String> filters, Pageable pageable) {		
		Page<SolrSample> pageSolrSample = solrSampleService.fetchSolrSampleByText(text, filters, pageable);		
		//for each result fetch the stored version and add e.g. inverse relationships		
		//stream process each solrSample into a sample *in parallel*
		Page<Sample> pageSample = new PageImpl<>(StreamSupport.stream(pageSolrSample.spliterator(), true)
					.map(ss->sampleService.fetch(ss.getAccession())).collect(Collectors.toList()), 
				pageable,pageSolrSample.getTotalElements()); 
				
		return pageSample;
	}
	

	//@Cacheable(cacheNames=WebappProperties.getSamplesOfExternalReference, sync=true)
	public Page<Sample> getSamplesOfExternalReference(String urlHash, Pageable pageable) {
		Page<NeoSample> pageNeoSample = neoSampleRepository.findByExternalReferenceUrlHash(urlHash, pageable);
		//get them in greater depth
		pageNeoSample.map(s -> neoSampleRepository.findOne(s.getAccession(), 2));		
		//convert them into a state to return
		Page<Sample> pageSample = pageNeoSample.map(neoSampleToSampleConverter);
		return pageSample;
	}

	//@Cacheable(cacheNames=WebappProperties.getSamplesOfCuration, sync=true)
	public Page<Sample> getSamplesOfCuration(String hash, Pageable pageable) {
		Page<NeoSample> pageNeoSample = neoSampleRepository.findByCurationHash(hash, pageable);
		//get them in greater depth
		pageNeoSample.map(s -> neoSampleRepository.findOne(s.getAccession(), 2));		
		//convert them into a state to return
		Page<Sample> pageSample = pageNeoSample.map(neoSampleToSampleConverter);
		return pageSample;
	}

	
	
}
