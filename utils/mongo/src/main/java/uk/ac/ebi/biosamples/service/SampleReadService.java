package uk.ac.ebi.biosamples.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

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
	
	private final MongoSampleRepository mongoSampleRepository;
	
	//TODO use a ConversionService to manage all these
	private final MongoSampleToSampleConverter mongoSampleToSampleConverter;
	
	private final CurationReadService curationReadService;
	
	private final ExecutorService executorService;
	
	public SampleReadService(MongoSampleRepository mongoSampleRepository,
			MongoSampleToSampleConverter mongoSampleToSampleConverter,
			CurationReadService curationReadService,
			BioSamplesProperties bioSamplesProperties) {
		this.mongoSampleRepository = mongoSampleRepository;
		this.mongoSampleToSampleConverter = mongoSampleToSampleConverter;
		this.curationReadService = curationReadService;
		executorService = AdaptiveThreadPoolExecutor.create(10000, 1000, false, 
				bioSamplesProperties.getBiosamplesCorePageThreadCount(), 
				bioSamplesProperties.getBiosamplesCorePageThreadCountMax());
	}
	
	/**
	 * Throws an IllegalArgumentException of no sample with that accession exists
	 * 
	 * @param accession
	 * @return
	 * @throws IllegalArgumentException
	 */
	//can't use a sync cache because we need to use CacheEvict
	//@Cacheable(cacheNames=WebappProperties.fetchUsing, key="#root.args[0]")
	public Optional<Sample> fetch(String accession, 
			Optional<List<String>> curationDomains) throws IllegalArgumentException {
		// return the raw sample from the repository
		MongoSample mongoSample = mongoSampleRepository.findOne(accession);
		if (mongoSample == null) {
			return Optional.empty();
		}
		
		try (Stream<MongoSample> stream = mongoSampleRepository.findAllByRelationshipsTarget(accession)) {
			  stream.forEach(relatedSample -> {
					for (MongoRelationship relationship : relatedSample.getRelationships()) {
						if (relationship.getTarget().equals(accession)) {
							mongoSample.getRelationships().add(relationship);
						}
					}
			  });
		}

		// convert it into the format to return
		Sample sample = mongoSampleToSampleConverter.convert(mongoSample);
		
		//add curation from a set of users
		//TODO limit curation to a set of users (possibly empty set)
		sample = curationReadService.applyAllCurationToSample(sample, curationDomains);
		
		
		return Optional.of(sample);
		
	}
	
	public Future<Optional<Sample>> fetchAsync(String accession, Optional<List<String>> curationDomains) {
		return executorService.submit(new FetchCallable(accession, this, curationDomains));
	}
	
	private static class FetchCallable implements Callable<Optional<Sample>> {

		private final SampleReadService sampleReadService;
		private final String accession;
		private final Optional<List<String>> curationDomains;
		
		public FetchCallable(String accession, SampleReadService sampleReadService, Optional<List<String>> curationDomains) {
			this.accession = accession;
			this.sampleReadService = sampleReadService;
			this.curationDomains = curationDomains;
		}
		
		@Override
		public Optional<Sample> call() throws Exception {
			return sampleReadService.fetch(accession, curationDomains);
		}		
	}
	
}
