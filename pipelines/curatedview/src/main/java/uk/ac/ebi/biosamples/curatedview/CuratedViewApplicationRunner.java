package uk.ac.ebi.biosamples.curatedview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ArgUtils;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Component
public class CuratedViewApplicationRunner implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(CuratedViewApplicationRunner.class);

    private final BioSamplesClient bioSamplesClient;
    private final PipelinesProperties pipelinesProperties;
    private final MongoSampleRepository repository;
    private final SampleToMongoSampleConverter sampleToMongoSampleConverter;

    public CuratedViewApplicationRunner(BioSamplesClient bioSamplesClient,
                                        PipelinesProperties pipelinesProperties,
                                        MongoSampleRepository repository,
                                        SampleToMongoSampleConverter sampleToMongoSampleConverter) {
        this.bioSamplesClient = bioSamplesClient;
        this.pipelinesProperties = pipelinesProperties;
        this.repository = repository;
        this.sampleToMongoSampleConverter = sampleToMongoSampleConverter;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Collection<Filter> filters = ArgUtils.getDateFilters(args);
        Instant startTime = Instant.now();
        LOG.info("Pipeline started at {}", startTime);
        long sampleCount = 0;

        try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true,
                pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

            Map<String, Future<Void>> futures = new HashMap<>();
            for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll("", filters)) {
                LOG.trace("Handling {}", sampleResource);
                Sample sample = sampleResource.getContent();
                Objects.requireNonNull(sample);

                Callable<Void> task = new CuratedViewCallable(sample, repository, sampleToMongoSampleConverter);
                futures.put(sample.getAccession(), executorService.submit(task));

                if (++sampleCount % 5000 == 0) {
                    LOG.info("Scheduled {} samples for processing", sampleCount);
                }
            }

            LOG.info("Waiting for all scheduled tasks to finish");
            ThreadUtils.checkFutures(futures, 0);
        } catch (Exception e) {
            LOG.error("Pipeline failed to finish successfully", e);
            throw e;
        } finally {
            logPipelineStat(startTime, sampleCount);
        }
    }

    private void logPipelineStat(Instant startTime, long sampleCount) {
        Instant endTime = Instant.now();
        LOG.info("Total samples processed {}", sampleCount);
        LOG.info("Pipeline finished at {}", endTime);
        LOG.info("Pipeline total running time {} seconds", Duration.between(startTime, endTime).getSeconds());
    }

}
