package uk.ac.ebi.biosamples.legacyxml;

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

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.service.XmlToSampleConverter;

@Component
public class MigrationRunner implements ApplicationRunner, ExitCodeGenerator {

	private final RestTemplate restTemplate;
	
	private int exitCode = 1;
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Value("${biosamples.pipelines.legacyxml.url:http://www.ebi.ac.uk/biosamples/xml/samples}")
	private String oldUrl;
	
	private final XmlToSampleConverter xmlToSampleConverter;
	private final BioSamplesClient client;
	
	public MigrationRunner(RestTemplateBuilder restTemplateBuilder, XmlToSampleConverter xmlToSampleConverter, BioSamplesClient client) {
		this.restTemplate = restTemplateBuilder.build();
		this.xmlToSampleConverter = xmlToSampleConverter;
		this.client = client;
	}
	
	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting MigrationRunner");
				
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		Queue<String> queue = new ArrayBlockingQueue<>(1024);
		AtomicBoolean finished = new AtomicBoolean(false);
		AccessFetcherCallable accessFetcherCallable = new AccessFetcherCallable(restTemplate, oldUrl, queue, finished);
		SampleCallable sampleCallable = new SampleCallable(restTemplate, oldUrl, queue, finished, xmlToSampleConverter, client);
		
		Future<Void> oldFuture = executorService.submit(accessFetcherCallable);
		Future<Void> sampleFuture = executorService.submit(sampleCallable);
				
		oldFuture.get();
		sampleFuture.get();
		executorService.shutdownNow();
		
		exitCode = 0;
		log.info("Finished MigrationRunner");
	}
}
