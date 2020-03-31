package uk.ac.ebi.biosamples.zooma;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.PipelineFutureCallback;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.AnalyticsService;
import uk.ac.ebi.biosamples.service.CurationApplicationService;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ArgUtils;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class ZoomaApplicationRunner implements ApplicationRunner {
	private static final Logger LOG = LoggerFactory.getLogger(ZoomaApplicationRunner.class);

	private final BioSamplesClient bioSamplesClient;	
	private final PipelinesProperties pipelinesProperties;
	private final ZoomaProcessor zoomaProcessor;
	private final CurationApplicationService curationApplicationService;
	private final AnalyticsService analyticsService;
	private final PipelineFutureCallback pipelineFutureCallback;
	
	public ZoomaApplicationRunner(BioSamplesClient bioSamplesClient, 
			PipelinesProperties pipelinesProperties, 
			ZoomaProcessor zoomaProcessor, 
			CurationApplicationService curationApplicationService,
								  AnalyticsService analyticsService) {
		this.bioSamplesClient = bioSamplesClient;
		this.pipelinesProperties = pipelinesProperties;
		this.zoomaProcessor = zoomaProcessor;
		this.curationApplicationService = curationApplicationService;
		this.analyticsService = analyticsService;
		this.pipelineFutureCallback = new PipelineFutureCallback();
	}
	
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		Instant startTime = Instant.now();
		Collection<Filter> filters = ArgUtils.getDateFilters(args);
		long sampleCount = 0;
		boolean isPassed = true;

		try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, 
				pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

			Map<String, Future<PipelineResult>> futures = new HashMap<>();
			
			for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll("", filters)) {
				LOG.trace("Handling "+sampleResource);
				Sample sample = sampleResource.getContent();
				if (sample == null) {
					throw new RuntimeException("Sample should not be null");
				}

				Callable<PipelineResult> task = new SampleZoomaCallable(bioSamplesClient, sample,
						zoomaProcessor, curationApplicationService, pipelinesProperties.getZoomaDomain());
				sampleCount++;
				
				futures.put(sample.getAccession(), executorService.submit(task));
			}
			
			LOG.info("waiting for futures");
			// wait for anything to finish
			ThreadUtils.checkAndCallbackFutures(futures, 0, pipelineFutureCallback);
		} catch (final Exception e) {
			LOG.error("Pipeline failed to finish successfully", e);
			isPassed = false;
		} finally {
			Instant endTime = Instant.now();
			LOG.info("Total samples processed {}", sampleCount);
			LOG.info("Total curation objects added {}", pipelineFutureCallback.getTotalCount());
			LOG.info("Pipeline finished at {}", endTime);
			LOG.info("Pipeline total running time {} seconds", Duration.between(startTime, endTime).getSeconds());

			PipelineAnalytics pipelineAnalytics = new PipelineAnalytics("zooma", startTime, endTime, sampleCount, pipelineFutureCallback.getTotalCount());
			pipelineAnalytics.setDateRange(filters);
			analyticsService.persistPipelineAnalytics(pipelineAnalytics);

			//now print a list of things that failed
			if (SampleZoomaCallable.failedQueue.size() > 0) {
				//put the first ones on the queue into a list
				//limit the size of list to avoid overload
				List<String> fails = new LinkedList<>();
				while (fails.size() < 100 && SampleZoomaCallable.failedQueue.peek() != null) {
					fails.add(SampleZoomaCallable.failedQueue.poll());
				}

				final String failures = "Failed files ("+fails.size()+") "+String.join(" , ", fails);

				LOG.info(failures);
				MailSender.sendEmail("Zooma", failures, isPassed);
			}
		}
	}

}
