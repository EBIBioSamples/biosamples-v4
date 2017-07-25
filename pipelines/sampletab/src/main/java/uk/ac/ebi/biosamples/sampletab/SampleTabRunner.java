package uk.ac.ebi.biosamples.sampletab;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class SampleTabRunner implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private PipelinesProperties pipelinesProperties;

	@Autowired
	private RestTemplateBuilder restTemplateBuilder;
	
	@Value("${biosamples.sampletab.path}")
	private String path;
	
	@Value("${biosamples.sampletab.uri}")
	private URI uri;
	
	@Override
	public void run(ApplicationArguments arg0) throws Exception {
		
		try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, 
				pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

			Map<String, Future<Void>> futures = new HashMap<>();
			
			SampleTabFileVisitor sampleTabFileVisitor = new SampleTabFileVisitor(executorService, futures, restTemplateBuilder.build(), uri);

		    Files.walkFileTree(Paths.get(path), sampleTabFileVisitor);
			
			log.info("waiting for futures");
			// wait for anything to finish
			ThreadUtils.checkFutures(futures, 0);
		}
		
		//now print a list of things that failed
		if (SampleTabCallable.failedQueue.size() > 0) {
			log.info("Failed files: "+String.join(" , ", SampleTabCallable.failedQueue));
		}

	}

}
