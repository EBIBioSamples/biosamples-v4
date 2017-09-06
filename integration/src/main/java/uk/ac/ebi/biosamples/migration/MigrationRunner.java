package uk.ac.ebi.biosamples.migration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.Sets;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Component
@Profile({"migration"})
public class MigrationRunner implements ApplicationRunner, ExitCodeGenerator {

	private final RestTemplate restTemplate;
	private ExecutorService executorService;
	private int exitCode = 1;
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public MigrationRunner(RestTemplateBuilder restTemplateBuilder) {
		restTemplate = restTemplateBuilder.build();
	}
	
	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting MigrationRunner");
		
		String oldUrl = "http://wwwdev.ebi.ac.uk/biosamples/xml/samples";
		String newUrl = "http://snowy.ebi.ac.uk:9083/biosamples/beta/xml/samples";//"http://wwwdev.ebi.ac.uk/biosamples/beta/xml/samples";
		
		try  {
			executorService = Executors.newFixedThreadPool(64);
			Queue<String> oldQueue = new ArrayBlockingQueue<>(128);
			AtomicBoolean oldFinished = new AtomicBoolean(false);
			AccessFetcherCallable oldCallable = new AccessFetcherCallable(restTemplate, oldUrl, oldQueue, oldFinished);
	
			Queue<String> newQueue = new ArrayBlockingQueue<>(128);
			AtomicBoolean newFinished = new AtomicBoolean(false);
			AccessFetcherCallable newCallable = new AccessFetcherCallable(restTemplate, newUrl, newQueue, newFinished);
	
			Queue<String> bothQueue = new ArrayBlockingQueue<>(128);
			AtomicBoolean bothFinished = new AtomicBoolean(false);
			
			AccessionQueueBothCallable bothCallable = new AccessionQueueBothCallable(oldQueue, oldFinished, newQueue, newFinished, bothQueue, bothFinished);
			
			AccessionComparisonCallable comparisonCallable = new AccessionComparisonCallable(restTemplate, oldUrl, newUrl, bothQueue, bothFinished);
			
			Future<Void> oldFuture = executorService.submit(oldCallable);
			Future<Void> newFuture = executorService.submit(newCallable);
			Future<Void> bothFuture = executorService.submit(bothCallable);
			Future<Void> comparisonFuture = executorService.submit(comparisonCallable);
					
			oldFuture.get();
			newFuture.get();
			bothFuture.get();
			comparisonFuture.get();
		} finally {
			executorService.shutdownNow();
		}
		
		exitCode = 0;
		log.info("Finished MigrationRunner");
	}
	
	private class AccessionQueueBothCallable implements Callable<Void> {
		
		private final Queue<String> oldQueue;
		private final Set<String> oldSet = new HashSet<>();
		private final AtomicBoolean oldFlag;
		private final Queue<String> newQueue;
		private final Set<String> newSet = new HashSet<>();
		private final AtomicBoolean newFlag;
		private final Queue<String> bothQueue;
		private final AtomicBoolean bothFlag;

		public AccessionQueueBothCallable(Queue<String> oldQueue, AtomicBoolean oldFlag, 
				Queue<String> newQueue, AtomicBoolean newFlag,
				Queue<String> bothQueue, AtomicBoolean bothFlag) {
			this.oldQueue = oldQueue;
			this.oldFlag = oldFlag;
			this.newQueue = newQueue;
			this.newFlag = newFlag;
			this.bothQueue = bothQueue;
			this.bothFlag = bothFlag;
		}
		
		@Override
		public Void call() throws Exception {
			log.info("Started AccessionQueueBothCallable.call(");
			
			while (!oldFlag.get() || !oldQueue.isEmpty() || !newFlag.get() || !newQueue.isEmpty()) {
				if (!oldFlag.get() || !oldQueue.isEmpty()) {
					String next = oldQueue.poll();
					if (next != null) {
						oldSet.add(next);
						if (newSet.contains(next)) {
							while (!bothQueue.offer(next)) {
								Thread.sleep(100);
							}
						}
					}
				}
				if (!newFlag.get() || !newQueue.isEmpty()) {
					String next = newQueue.poll();
					if (next != null) {
						newSet.add(next);
						if (oldSet.contains(next)) {
							while (!bothQueue.offer(next)) {
								Thread.sleep(100);
							}
						}
					}
				}
			}
			
			//at his point we should be able to generate the differences in the sets
			
			Set<String> newOnly = Sets.difference(newSet, oldSet);
			Set<String> oldOnly = Sets.difference(oldSet, newSet);
			log.info("Samples only in new "+newOnly.size());
			log.info("Samples only in old "+oldOnly.size());
			
			
			bothFlag.set(true);
			log.info("Finished AccessionQueueBothCallable.call(");
			
			return null;
		}
		
	}
}
