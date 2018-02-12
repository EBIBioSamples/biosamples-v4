package uk.ac.ebi.biosamples.service;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
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
public class SampleService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	//TODO use constructor injection
	
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
	private SampleReadService sampleReadService;
	
	@Autowired
	private MessagingService messagingSerivce;
	
	/**
	 * Throws an IllegalArgumentException of no sample with that accession exists
	 * 
	 * @param accession
	 * @return
	 * @throws IllegalArgumentException
	 */
	//can't use a sync cache because we need to use CacheEvict
	//@Cacheable(cacheNames=WebappProperties.fetchUsing, key="#root.args[0]")
	public Optional<Sample> fetch(String accession) {
		return sampleReadService.fetch(accession);
	}
	
	
	public Autocomplete getAutocomplete(String autocompletePrefix, Collection<Filter> filters, int noSuggestions) {
		return solrSampleService.getAutocomplete(autocompletePrefix, filters, noSuggestions);
	}

	//because the fetchUsing caches the sample, if an updated version is stored, we need to make sure that any cached version
	//is removed. 
	//Note, pages of samples will not be cache busted, only single-accession sample retrieval
	//@CacheEvict(cacheNames=WebappProperties.fetchUsing, key="#result.accession")
	public Sample store(Sample sample) {
		// TODO check if there is an existing copy and if there are any changes

		//do validation
		// TODO validate that relationships have this sample as the source 
		Collection<String> errors = sampleValidator.validate(sample);
		if (errors.size() > 0) {
			//TODO no validation information is provided to users
			log.error("Found errors : "+errors);
			throw new SampleValidationException();
		}

		if (sample.hasAccession()) {

			// TODO compare to existing version to check if changes
			MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);
			mongoSample = mongoSampleRepository.save(mongoSample);
			sample = mongoSampleToSampleConverter.convert(mongoSample);

			// send a message for storage and further processing
			messagingSerivce.sendMessages(sample);
		} else {
			//assign it a new accession
			sample = mongoAccessionService.generateAccession(sample);
			
			//update its relationships

			//send a message for storage and further processing
			messagingSerivce.sendMessages(sample);
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
	/*
	//this code recursively follows relationships
	//TODO finish
	public SortedSet<Sample> getRelated(Sample sample, String relationshipType) {
		Queue<String> toCheck = new LinkedList<>();
		Set<String> checked = new HashSet<>();
		Collection<Sample> related = new TreeSet<>();
		toCheck.add(sample.getAccession());
		while (!toCheck.isEmpty()) {
			String accessionToCheck = toCheck.poll();
			checked.add(accessionToCheck);
			Sample sampleToCheck = sampleReadService.fetchUsing(accessionToCheck);
			related.add(sampleToCheck);
			for (Relationship rel : sampleToCheck.getRelationships()) {
				if (relationshipType == null || relationshipType.equals(rel.getType())) {
					if (!checked.contains(rel.getSource()) && toCheck.contains(rel.getSource())) {
						toCheck.add(rel.getSource());
					}
					if (!checked.contains(rel.getTarget()) && toCheck.contains(rel.getTarget())) {
						toCheck.add(rel.getTarget());
					}
				}
			}
		}
		related.remove(sample);
		return related;
	}
	*/
}
