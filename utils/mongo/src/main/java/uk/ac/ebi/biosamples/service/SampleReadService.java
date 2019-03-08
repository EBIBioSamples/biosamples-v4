package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoInverseRelationshipService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Service layer business logic for centralising repository access and
 * conversions between different controller. Use this instead of linking to
 * repositories directly.
 *
 * @author faulcon
 */
@Service
public class SampleReadService {

    private static Logger LOGGER = LoggerFactory.getLogger(SampleReadService.class);

    private final MongoSampleRepository mongoSampleRepository;

    //TODO use a ConversionService to manage all these
    private final MongoSampleToSampleConverter mongoSampleToSampleConverter;

    private final CurationReadService curationReadService;
    private final MongoInverseRelationshipService mongoInverseRelationshipService;

    private final ExecutorService executorService;

    public SampleReadService(MongoSampleRepository mongoSampleRepository,
                             MongoSampleToSampleConverter mongoSampleToSampleConverter,
                             CurationReadService curationReadService,
                             MongoInverseRelationshipService mongoInverseRelationshipService,
                             BioSamplesProperties bioSamplesProperties) {
        this.mongoSampleRepository = mongoSampleRepository;
        this.mongoSampleToSampleConverter = mongoSampleToSampleConverter;
        this.curationReadService = curationReadService;
        this.mongoInverseRelationshipService = mongoInverseRelationshipService;
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
        // return the sample from the repository
        long startTime, endTime;

        startTime = System.nanoTime();
        MongoSample mongoSample = mongoSampleRepository.findOne(accession);
        if (mongoSample == null) {
            LOGGER.warn(String.format("failed to retrieve sample with accession %s", accession));
            return Optional.empty();
        }
        endTime = System.nanoTime();
        LOGGER.trace("Got mongo original " + accession + " in " + ((endTime - startTime) / 1000000) + "ms");

        //add on inverse relationships
        startTime = System.nanoTime();
        mongoSample = mongoInverseRelationshipService.addInverseRelationships(mongoSample);
        endTime = System.nanoTime();
        LOGGER.trace("Got inverse relationships " + accession + " in " + ((endTime - startTime) / 1000000) + "ms");

        // convert it into the format to return
        Sample sample = mongoSampleToSampleConverter.convert(mongoSample);

        //add curation from a set of users
        startTime = System.nanoTime();
        sample = curationReadService.applyAllCurationToSample(sample, curationDomains);
        endTime = System.nanoTime();
        LOGGER.trace("Applied curation to " + accession + " in " + ((endTime - startTime) / 1000000) + "ms");


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
            Optional<Sample> opt = sampleReadService.fetch(accession, curationDomains);
            if (!opt.isPresent()) {
                LOGGER.warn(String.format("failed to retrieve sample with accession %s", accession));
            }
            return opt;
        }
    }

}
