package uk.ac.ebi.biosamples.migration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.google.common.collect.Sets;

import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;

@Component
@Profile({"migration"})
public class MigrationRunner implements ApplicationRunner, ExitCodeGenerator {

	private final RestTemplate restTemplate;
	private ExecutorService executorService;
	
	private int exitCode = 1;
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Value("${biosamples.migration.old:http://www.ebi.ac.uk/biosamples/xml/samples}")
	private String oldUrl;
	@Value("${biosamples.migration.new:http://www.ebi.ac.uk/biosamples/xml/samples}")
	private String newUrl;
	
	private final XmlSampleToSampleConverter xmlToSampleConverter;
	
	public MigrationRunner(RestTemplateBuilder restTemplateBuilder, XmlSampleToSampleConverter xmlToSampleConverter) {
		restTemplate = restTemplateBuilder.build();
		this.xmlToSampleConverter = xmlToSampleConverter;
	}
	
	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting MigrationRunner");
		
		//String oldUrl = "http://www.ebi.ac.uk/biosamples/xml/samples";
		//String newUrl = "http://localhost:8083/biosamples/beta/xml/samples";

		//String oldUrl = "http://beans.ebi.ac.uk:9480/biosamples/xml/samples";
		//String newUrl = "http://scooby.ebi.ac.uk:8083/biosamples/beta/xml/samples";
		
		//String newUrl = "http://wwwdev.ebi.ac.uk/biosamples/beta/xml/samples";
		//String newUrl = "http://snowy.ebi.ac.uk:9083/biosamples/beta/xml/samples";
		
		
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
			
			AccessionQueueBothCallable bothCallable = new AccessionQueueBothCallable(oldQueue, oldFinished, 
					newQueue, newFinished, bothQueue, bothFinished);
			
			AccessionComparisonCallable comparisonCallable = new AccessionComparisonCallable(restTemplate, 
					oldUrl, newUrl, bothQueue, bothFinished, xmlToSampleConverter, args.containsOption("--comparison"));
			
//			comparisonCallable.compare("SAMEA3683023");
			
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
}
