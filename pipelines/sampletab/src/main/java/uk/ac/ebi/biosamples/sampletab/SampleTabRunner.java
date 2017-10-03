package uk.ac.ebi.biosamples.sampletab;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
	public void run(ApplicationArguments args) throws Exception {

		// date format is YYYY-mm-dd
		LocalDate fromDate = null;
		if (args.getOptionNames().contains("from")) {
			fromDate = LocalDate.parse(args.getOptionValues("from").iterator().next(),
					DateTimeFormatter.ISO_LOCAL_DATE);
		} else {
			fromDate = LocalDate.parse("1000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
		}
		LocalDate toDate = null;
		if (args.getOptionNames().contains("until")) {
			toDate = LocalDate.parse(args.getOptionValues("until").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
		} else {
			toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
		}
		
		try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, 
				pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

			Map<String, Future<Void>> futures = new LinkedHashMap<>();
			
			SampleTabFileVisitor sampleTabFileVisitor = new SampleTabFileVisitor(executorService, futures, 
					restTemplateBuilder.build(), uri, fromDate, toDate);

		    Files.walkFileTree(Paths.get(path), sampleTabFileVisitor);
			
			log.info("waiting for futures");
			// wait for anything to finish
			ThreadUtils.checkFutures(futures, 0);
		} finally {
			//now print a list of things that failed
			if (SampleTabCallable.failedQueue.size() > 0) {
				log.info("Failed files: "+String.join(" , ", SampleTabCallable.failedQueue));
			}
		}
	}

}
