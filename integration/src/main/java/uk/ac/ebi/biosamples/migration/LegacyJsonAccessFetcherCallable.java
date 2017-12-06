package uk.ac.ebi.biosamples.migration;

import com.jayway.jsonpath.JsonPath;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LegacyJsonAccessFetcherCallable implements Callable<Void> {

	private final RestTemplate restTemplate;
	private final String rootUrl;
	private final Queue<String> accessionQueue;
	private final AtomicBoolean finishFlag;
	private final Logger log = LoggerFactory.getLogger(getClass());

	public LegacyJsonAccessFetcherCallable(RestTemplate restTemplate, String rootUrl, Queue<String> accessionQueue, AtomicBoolean finishFlag) {
		this.restTemplate = restTemplate;
		this.rootUrl = rootUrl;
		this.accessionQueue = accessionQueue;
		this.finishFlag = finishFlag;
	}
	
	@Override
	public Void call() throws Exception {
		log.info("Started against "+rootUrl);

		long oldTime = System.nanoTime();		
		int pagesize = 1000;
		
		ExecutorService executorService = null;
		
		try {
			executorService = Executors.newFixedThreadPool(32);
//			executorService = Executors.newFixedThreadPool(1);
			getPages("samples", pagesize, executorService);
			getPages("groups", pagesize, executorService);			
		} finally {
			executorService.shutdownNow();
		}
		finishFlag.set(true);
		long elapsed = System.nanoTime()-oldTime;
		log.info("Collected from "+rootUrl+" in "+(elapsed/1000000000l)+"s");
		
		log.info("Finished AccessFetcherCallable.call(");
		
		return null;		
	}
	
	private void getPages(String pathSegment, int pagesize, ExecutorService executorService) throws DocumentException, InterruptedException, ExecutionException {

		UriComponentsBuilder uriComponentBuilder = UriComponentsBuilder.fromUriString(rootUrl)
				.pathSegment(pathSegment);
			uriComponentBuilder.replaceQueryParam("size", pagesize);

		//get the first page to get the number of pages in total
//		uriComponentBuilder.replaceQueryParam("page", 1);
		URI uri = uriComponentBuilder.build().toUri();

		ResponseEntity<String> response;
		RequestEntity<?> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		try {
			response = restTemplate.exchange(request, String.class);
		} catch (RestClientException e) {
			log.error("Problem accessing "+uri, e);
			throw e;
		}
		String jsonString = response.getBody();

		int pageCount = JsonPath.read(jsonString, "$.page.totalPages");
		
		//multi-thread all the other pages via futures
		List<Future<Set<String>>> futures = new ArrayList<>();
		for (int i = 0; i <= pageCount; i++) {
			uriComponentBuilder.replaceQueryParam("page", i);			
			URI pageUri = uriComponentBuilder.build().toUri();
			Callable<Set<String>> callable =  getPageCallable(pageUri);
			futures.add(executorService.submit(callable));
		}
		for (Future<Set<String>> future : futures) {
            for (String accession : future.get()) {
                while (!accessionQueue.offer(accession)) {
                    Thread.sleep(10);
                }
            }
		}
	}
	
	public Callable<Set<String>> getPageCallable(URI uri) {
		return new Callable<Set<String>>(){

			@Override
			public Set<String> call() throws Exception {
				long startTime = System.nanoTime();
				ResponseEntity<String> response;
				RequestEntity<?> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
				try {
					response = restTemplate.exchange(request, String.class);
				} catch (RestClientException e) {
					log.error("Problem accessing "+uri, e);
					throw e;
				}
				String jsonString = response.getBody();
				long endTime = System.nanoTime();
				long interval = (endTime-startTime)/1000000l;
				log.info("Got "+uri+" in "+interval+"ms");

				List<String> accessions = JsonPath.read(jsonString, "$._embedded.samples.*.accession");
				return new HashSet<>(accessions);
			}
		};
	}
	
}