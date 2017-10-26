package uk.ac.ebi.biosamples.legacyxml;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
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
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class AccessFetcherCallable implements Callable<Void> {

	private final RestTemplate restTemplate;
	private final String rootUrl;
	private final Queue<String> accessionQueue;
	private final AtomicBoolean finishFlag;
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public AccessFetcherCallable(RestTemplate restTemplate, String rootUrl, Queue<String> accessionQueue, AtomicBoolean finishFlag) {
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
		
			UriComponentsBuilder uriComponentBuilder = UriComponentsBuilder.fromUriString(rootUrl);
			uriComponentBuilder.replaceQueryParam("pagesize", pagesize);
			uriComponentBuilder.replaceQueryParam("query", "");
			
			int pageCount = getPageCount(uriComponentBuilder, pagesize);
			
			//multi-thread the pages via futures
			Map<String, Future<Void>> futures = new HashMap<>();
			for (int i = 1; i <= pageCount; i++) {
				uriComponentBuilder.replaceQueryParam("page", i);			
				URI pageUri = uriComponentBuilder.build().toUri();
				Future<Void> future = executorService.submit(getPageCallable(pageUri, accessionQueue));
				futures.put(pageUri.toString(), future);
				ThreadUtils.checkFutures(futures, 0);
			}

		} finally {
			executorService.shutdownNow();
		}
		finishFlag.set(true);
		long elapsed = System.nanoTime()-oldTime;
		log.info("Collected from "+rootUrl+" in "+(elapsed/1000000000l)+"s");
		
		log.info("Finished AccessFetcherCallable.call(");
		
		return null;
	}
	
	public int getPageCount(UriComponentsBuilder uriComponentBuilder, int pagesize) throws DocumentException {
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
		
		return pageCount;
		
	}
	
	public Callable<Void> getPageCallable(URI uri, Queue<String> queue) {
		return new Callable<Void>(){

			@Override
			public Void call() throws Exception {
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
				
				for (Element element : XmlPathBuilder.of(root).elements("BioSample")) {
					String accession = element.attributeValue("id"); 
					//only handle sample accessions for now
					if (!accession.startsWith("SAMEG")) {
						while (!queue.offer(accession)) {
							Thread.sleep(10);
						}
					}
				}
				
				return null;
			}
		};
	}
	
}