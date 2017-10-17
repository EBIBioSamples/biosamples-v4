package uk.ac.ebi.biosamples.service;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;

/**
 * Service layer business logic for centralising repository access and
 * conversions between different controller. Use this instead of linking to
 * repositories directly.
 * 
 * @author faulcon
 *
 */
@Service
public class SampleReadService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	//TODO use constructor injection not dependency injection
	
	@Autowired
	private MongoSampleRepository mongoSampleRepository;
	
	//TODO use a ConversionService to manage all these
	@Autowired
	private MongoSampleToSampleConverter mongoSampleToSampleConverter;
	
	@Autowired
	private CurationReadService curationReadService;
	
	//TODO application.properties this
	
	private ExecutorService executorService = Executors.newFixedThreadPool(64);
	
	/**
	 * Throws an IllegalArgumentException of no sample with that accession exists
	 * 
	 * @param accession
	 * @return
	 * @throws IllegalArgumentException
	 */
	//can't use a sync cache because we need to use CacheEvict
	//@Cacheable(cacheNames=WebappProperties.fetch, key="#root.args[0]")
	public Optional<Sample> fetch(String accession) throws IllegalArgumentException {
		// return the raw sample from the repository
		MongoSample mongoSample = mongoSampleRepository.findOne(accession);
		if (mongoSample == null) {
			return Optional.empty();
		}
		//TODO only have relationships to things that are accessible

		// convert it into the format to return
		Sample sample = mongoSampleToSampleConverter.convert(mongoSample);
		
		//add curation from a set of users
		//TODO limit curation to a set of users (possibly empty set)
		sample = curationReadService.applyAllCurationToSample(sample);
		
		
		return Optional.of(sample);
		
	}
	
	public Future<Optional<Sample>> fetchAsync(String accession) {
		return executorService.submit(new FetchCallable(accession, this));
	}
	
	private static class FetchCallable implements Callable<Optional<Sample>> {

		private final SampleReadService sampleReadService;
		private final String accession;
		
		public FetchCallable(String accession, SampleReadService sampleReadService) {
			this.accession = accession;
			this.sampleReadService = sampleReadService;
		}
		
		@Override
		public Optional<Sample> call() throws Exception {
			return sampleReadService.fetch(accession);
		}		
	}
	
}
