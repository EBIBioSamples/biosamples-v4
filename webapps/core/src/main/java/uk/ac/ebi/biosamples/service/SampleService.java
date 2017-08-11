package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer business logic for centralising repository access and
 * conversions between different controller. Use this instead of linking to
 * repositories directly.
 * 
 * @author faulcon
 *
 */
@Service
public class SampleService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private MongoAccessionService mongoAccessionService;
	@Autowired
	private MongoSampleRepository mongoSampleRepository;			
	@Autowired
	private MongoSampleToSampleConverter mongoSampleToSampleConverter;
	@Autowired
	private SampleToMongoSampleConverter sampleToMongoSampleConverter;			
	
	
	@Autowired 
	private SampleValidator sampleValidator;
	
	@Autowired
	private SolrSampleService solrSampleService;

	@Autowired
	private CurationReadService curationReadService;

	@Autowired
	private AmqpTemplate amqpTemplate;
	
	@Autowired
	private SampleReadService sampleReadService;
	
	/**
	 * Throws an IllegalArgumentException of no sample with that accession exists
	 * 
	 * @param accession
	 * @return
	 * @throws IllegalArgumentException
	 */
	//can't use a sync cache because we need to use CacheEvict
	//@Cacheable(cacheNames=WebappProperties.fetch, key="#root.args[0]")
	public Sample fetch(String accession) throws IllegalArgumentException {
		return sampleReadService.fetch(accession);
	}
	
	
	public Autocomplete getAutocomplete(String autocompletePrefix, MultiValueMap<String,String> filters, int noSuggestions) {
		return solrSampleService.getAutocomplete(autocompletePrefix, filters, noSuggestions);
	}

	//because the fetch caches the sample, if an updated version is stored, we need to make sure that any cached version
	//is removed. 
	//Note, pages of samples will not be cache busted, only single-accession sample retrieval
	//@CacheEvict(cacheNames=WebappProperties.fetch, key="#result.accession")
	public Sample store(Sample sample) {
		// TODO check if there is an existing copy and if there are any changes

		//do validation
		Collection<String> errors = sampleValidator.validate(sample);
		if (errors.size() > 0) {
			//TODO no validation information is provided to users
			log.error("Found errors : "+errors);
			throw new SampleValidationException();
		}
				
		// TODO validate that relationships have this sample as the source 
		sample = Sample.build(sample.getName(), sample.getAccession(), sample.getRelease(), LocalDateTime.now(),
				sample.getCharacteristics(), sample.getRelationships(), sample.getExternalReferences());

		if (sample.hasAccession()) {
			MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);
			mongoSample = mongoSampleRepository.save(mongoSample);
			sample = mongoSampleToSampleConverter.convert(mongoSample);
		} else {
			//assign it a new accession
			sample = mongoAccessionService.generateAccession(sample);
		}

		List<String> relatedSampleAccession = SampleRelationshipUtils.getOutgoingRelationships(sample).stream().map(Relationship::getTarget).collect(Collectors.toList());
		List<Sample> relatedSamples = new ArrayList<>();
		for (String accession : relatedSampleAccession) {
			Optional.ofNullable(mongoSampleRepository.findOne(accession)).ifPresent(mongoSample ->relatedSamples.add(mongoSampleToSampleConverter.convert(mongoSample)));
		}

		// send a message for storage and further processing
		//TODO put in eventlistener
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexing, "", MessageContent.build(sample, false));
		for(Sample relatedSample: relatedSamples) {
			amqpTemplate.convertAndSend(Messaging.exchangeForIndexing, "", MessageContent.build(relatedSample, false));
		}

		//return the sample in case we have modified it i.e accessioned
		return sample;
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public class SampleValidationException extends RuntimeException {
		private static final long serialVersionUID = -7937033504537036300L;

		public SampleValidationException() {
			super();
		}

		public SampleValidationException(String message, Throwable cause, boolean enableSuppression,
				boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public SampleValidationException(String message, Throwable cause) {
			super(message, cause);
		}

		public SampleValidationException(String message) {
			super(message);
		}

		public SampleValidationException(Throwable cause) {
			super(cause);
		}
	}
	
}
