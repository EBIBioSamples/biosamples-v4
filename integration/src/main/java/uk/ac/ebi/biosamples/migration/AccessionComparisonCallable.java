package uk.ac.ebi.biosamples.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.biosamples.service.XmlGroupToSampleConverter;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

class AccessionComparisonCallable implements Callable<Void> {
	private final RestTemplate restTemplate;
	private final String oldUrl;
	private final String newUrl;
	private final Queue<String> bothQueue;
	private final AtomicBoolean bothFlag;
	private final XmlSampleToSampleConverter xmlSampleToSampleConverter;
	private final XmlGroupToSampleConverter xmlGroupToSampleConverter;
	private final boolean compare;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public AccessionComparisonCallable(RestTemplate restTemplate, String oldUrl, String newUrl, Queue<String> bothQueue,
			AtomicBoolean bothFlag, 
			XmlSampleToSampleConverter xmlSampleToSampleConverter, XmlGroupToSampleConverter xmlGroupToSampleConverter, 
			boolean compare) {
		this.restTemplate = restTemplate;
		this.oldUrl = oldUrl;
		this.newUrl = newUrl;
		this.bothQueue = bothQueue;
		this.bothFlag = bothFlag;
		this.xmlSampleToSampleConverter = xmlSampleToSampleConverter;
		this.xmlGroupToSampleConverter = xmlGroupToSampleConverter;
		this.compare = compare;
	}

	@Override
	public Void call() throws Exception {
		log.info("Started");
		log.info("oldUrl = "+oldUrl);
		log.info("newUrl = "+newUrl);
		log.info("compare = "+compare);
		SortedSet<String> problemAccessions = new TreeSet<>();
		Map<String, Future<Void>> compareFutures = new HashMap<>();
		ExecutorService executorService = null;

		try {
			//executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, 1, 32);
			executorService = Executors.newFixedThreadPool(8);
			while (!bothFlag.get() || !bothQueue.isEmpty()) {
				String accession = bothQueue.poll();
				if (accession != null) {
					if (compare) {
						log.info("Comparing accession "+ accession);
						compareFutures.put(accession, executorService.submit(new CompareCallable(accession, oldUrl, newUrl, 
								xmlSampleToSampleConverter, xmlGroupToSampleConverter, restTemplate)));
	
						//make sure we don't have too many futures
						ThreadUtils.checkFutures(compareFutures, 100);
					}
				} else {
					Thread.sleep(100);
				}
			}
			ThreadUtils.checkFutures(compareFutures, 0);
		} finally {
			if (executorService != null) {
				executorService.shutdownNow();
			}
		}
		for (String accession : problemAccessions) {
			log.error("Problem accessing "+accession);
		}
		log.info("Finished AccessionComparisonCallable.call(");
		return null;
	}
	
}