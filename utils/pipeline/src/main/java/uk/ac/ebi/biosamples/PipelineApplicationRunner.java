package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.AnalyticsService;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ArgUtils;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public abstract class PipelineApplicationRunner implements ApplicationRunner {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());
    private static final String PIPELINE_NAME = "TEMPLATE";

    protected final BioSamplesClient bioSamplesClient;
    private final PipelinesProperties pipelinesProperties;
    private final AnalyticsService analyticsService;
    private final PipelineFutureCallback pipelineFutureCallback;

    public PipelineApplicationRunner(BioSamplesClient bioSamplesClient,
                                     PipelinesProperties pipelinesProperties,
                                     AnalyticsService analyticsService) {
        this.bioSamplesClient = bioSamplesClient;
        this.pipelinesProperties = pipelinesProperties;
        this.analyticsService = analyticsService;
        this.pipelineFutureCallback = new PipelineFutureCallback();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Instant startTime = Instant.now();
        LOG.info("Pipeline started at {}", startTime);
        Collection<Filter> filters = ArgUtils.getDateFilters(args);
        long sampleCount = 0;

        loadPreConfiguration();

        try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true,
                pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

            Map<String, Future<PipelineResult>> futures = new HashMap<>();
            for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll("", filters)) {
                Sample sample = Objects.requireNonNull(sampleResource.getContent());
                LOG.trace("Handling {}", sample);

                Callable<PipelineResult> task = getNewCallableClassInstance().withSample(sample);
                sampleCount++;
                if (sampleCount % 10000 == 0) {
                    LOG.info("{} samples scheduled for processing", sampleCount);
                }
                futures.put(sample.getAccession(), executorService.submit(task));
            }

            LOG.info("waiting for futures to finish");
            ThreadUtils.checkAndCallbackFutures(futures, 0, pipelineFutureCallback);
        } catch (final Exception e) {
            LOG.error("Pipeline failed to finish successfully", e);
            MailSender.sendEmail(getPipelineName(), "Failed for network connectivity issues/ other issues - <ALERT BIOSAMPLES DEV> " + e.getMessage(), false);
            throw e;
        } finally {
            Instant endTime = Instant.now();
            LOG.info("Total samples processed {}", sampleCount);
            LOG.info("Total curation objects added {}", pipelineFutureCallback.getTotalCount());
            LOG.info("Pipeline finished at {}", endTime);
            LOG.info("Pipeline total running time {} seconds", Duration.between(startTime, endTime).getSeconds());

            PipelineAnalytics pipelineAnalytics = new PipelineAnalytics(getPipelineName(), startTime, endTime, sampleCount, pipelineFutureCallback.getTotalCount());
            pipelineAnalytics.setDateRange(filters);
            analyticsService.persistPipelineAnalytics(pipelineAnalytics);

            MailSender.sendEmail(getPipelineName(), String.join(",", pipelineFutureCallback.getFailedSamples()), pipelineFutureCallback.getFailedSamples().isEmpty());
        }
    }

    protected String getPipelineName() {
        return PIPELINE_NAME;
    }

    public abstract void loadPreConfiguration();
    public abstract PipelineSampleCallable getNewCallableClassInstance();
}
