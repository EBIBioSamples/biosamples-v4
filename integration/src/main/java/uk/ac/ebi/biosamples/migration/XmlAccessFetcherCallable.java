package uk.ac.ebi.biosamples.migration;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class XmlAccessFetcherCallable implements Callable<Void> {

	private final RestTemplate restTemplate;
	private final String rootUrl;
	private final Queue<String> accessionQueue;
	private final AtomicBoolean finishFlag;
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public XmlAccessFetcherCallable(RestTemplate restTemplate, String rootUrl, Queue<String> accessionQueue, AtomicBoolean finishFlag) {
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
			executorService = Executors.newFixedThreadPool(4);		
			getPages("samples", pagesize, executorService, "");
			getPages("groups", pagesize, executorService, "groups");			
		} finally {
			executorService.shutdownNow();
		}
		finishFlag.set(true);
		long elapsed = System.nanoTime()-oldTime;
		log.info("Collected from "+rootUrl+" in "+(elapsed/1000000000l)+"s");
		
		log.info("Finished AccessFetcherCallable.call(");
		
		return null;		
	}
	
	private void getPages(String pathSegment, int pagesize, ExecutorService executorService, String query) throws DocumentException, InterruptedException, ExecutionException {

		UriComponentsBuilder uriComponentBuilder = UriComponentsBuilder.fromUriString(rootUrl)
				.pathSegment(pathSegment);
			uriComponentBuilder.replaceQueryParam("pagesize", pagesize);
			uriComponentBuilder.replaceQueryParam("query", query);
		
		//get the first page to get the number of pages in total
		uriComponentBuilder.replaceQueryParam("page", 1);			
		URI uri = uriComponentBuilder.build().toUri();

		ResponseEntity<String> response;
		RequestEntity<?> request = RequestEntity.get(uri).accept(MediaType.TEXT_XML).build();
		try {
			response = restTemplate.exchange(request, String.class);
		} catch (RestClientException e) {
			log.error("Problem accessing "+uri, e);
			throw e;
		}
		String xmlString = response.getBody();
		
		SAXReader reader = new SAXReader();
		Document xml = reader.read(new StringReader(xmlString));
		Element root = xml.getRootElement();		
		
		int pageCount = (Integer.parseInt(XmlPathBuilder.of(root).path("SummaryInfo", "Total").text())/pagesize)+1;
		
		
		//multi-thread all the other pages via futures
		List<Future<Set<String>>> futures = new ArrayList<>();
		for (int i = 1; i <= pageCount; i++) {
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
				RequestEntity<?> request = RequestEntity.get(uri).accept(MediaType.TEXT_XML).build();
				try {
					response = restTemplate.exchange(request, String.class);
				} catch (RestClientException e) {
					log.error("Problem accessing "+uri, e);
					throw e;
				}
				String xmlString = response.getBody();
				long endTime = System.nanoTime();
				long interval = (endTime-startTime)/1000000l;
				log.info("Got "+uri+" in "+interval+"ms");
				
				SAXReader reader = new SAXReader();
				Document xml = reader.read(new StringReader(xmlString));
				Element root = xml.getRootElement();					
				
				Set<String> accessions = new HashSet<>();
				
				for (Element element : XmlPathBuilder.of(root).elements("BioSample")) {
					String accession = element.attributeValue("id"); 
					//only handle sample accessions for now
					if (!accession.startsWith("SAMEG")) {
						accessions.add(accession);
					}
				}
				
				return accessions;
			}
		};
	}
	
}