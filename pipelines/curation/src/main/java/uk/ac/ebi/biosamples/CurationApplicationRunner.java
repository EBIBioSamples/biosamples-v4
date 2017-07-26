package uk.ac.ebi.biosamples;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.zooma.ZoomaProcessor;

@Component
public class CurationApplicationRunner implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final BioSamplesClient bioSamplesClient;	
	private final PipelinesProperties pipelinesProperties;
	private final ZoomaProcessor zoomaProcessor;
	
	public CurationApplicationRunner(BioSamplesClient bioSamplesClient, PipelinesProperties pipelinesProperties, ZoomaProcessor zoomaProcessor) {
		this.bioSamplesClient = bioSamplesClient;
		this.pipelinesProperties = pipelinesProperties;
		this.zoomaProcessor = zoomaProcessor;
	}
	
	
	@Override
	public void run(ApplicationArguments arg0) throws Exception {

		try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, 
				pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

			Map<String, Future<Void>> futures = new HashMap<>();
			
			for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll()) {
				
				Sample sample = sampleResource.getContent();
				if (sample == null) {
					throw new RuntimeException("Sample should not be null");
				}

				Callable<Void> task = new SampleCurationCallable(bioSamplesClient, sample, zoomaProcessor);
				
				futures.put(sample.getAccession(), executorService.submit(task));
			}
			
			log.info("waiting for futures");
			// wait for anything to finish
			ThreadUtils.checkFutures(futures, 0);
		} finally {
			//now print a list of things that failed
			if (SampleCurationCallable.failedQueue.size() > 0) {
				log.info("Failed files: "+String.join(" , ", SampleCurationCallable.failedQueue));
			}
		}
	}

}
