package uk.ac.ebi.biosamples.legacyxml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.opencsv.CSVWriter;

import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Component
public class ToFileRunner implements ApplicationRunner {

	private final RestTemplate restTemplate;
	private final Logger log = LoggerFactory.getLogger(getClass());

	int pagesize = 1000;
	private final Map<String, Future<Collection<String>>> pageFutures = new HashMap<>();
	private final Map<String, Future<String>> accessionFutures = new HashMap<>();

	private PageCallback pageCallback = null;
	private AccessionCallback accessionCallback = null;
	
	public ToFileRunner(RestTemplateBuilder restTemplate) {
		this.restTemplate = restTemplate.build();
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		if (!"export".equals(args.getNonOptionArgs().get(0))) {
			return;
		}
		
		String rootUrl = args.getNonOptionArgs().get(1);
		String outputXmlFilename = args.getNonOptionArgs().get(2);

		long oldTime = System.nanoTime();		
		
		ExecutorService pageExecutorService = null;
		ExecutorService accessionExecutorService = null;
		
		try {
			pageExecutorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, 1, 32);		
			accessionExecutorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, 1, 32);
			try (FileWriter fileXmlWriter = new FileWriter(new File(outputXmlFilename))) {
					
					fileXmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
					fileXmlWriter.write("<BioSamples>\n");
					
					accessionCallback = new AccessionCallback(fileXmlWriter);		
					pageCallback = new PageCallback(accessionExecutorService, rootUrl, 
							restTemplate, accessionCallback, accessionFutures);
	
					UriComponentsBuilder pageUriComponentBuilder;
					int pageCount;
					
					//handle samples
					pageUriComponentBuilder = UriComponentsBuilder.fromUriString(rootUrl);
					pageUriComponentBuilder.pathSegment("samples");
					pageUriComponentBuilder.replaceQueryParam("pagesize", pagesize);
					pageUriComponentBuilder.replaceQueryParam("query", "");
					
					pageCount = getPageCount(pageUriComponentBuilder, pagesize);
					
					//multi-thread the pages via futures
					for (int i = 1; i <= pageCount; i++) {
						pageUriComponentBuilder.replaceQueryParam("page", i);			
						URI pageUri = pageUriComponentBuilder.build().toUri();
						Future<Collection<String>> future = pageExecutorService.submit(getPageCallable(pageUri));
						pageFutures.put(pageUri.toString(), future);
						ThreadUtils.checkAndCallbackFutures(pageFutures, 100, pageCallback);
					}
					
					//handle groups
					pageUriComponentBuilder = UriComponentsBuilder.fromUriString(rootUrl);
					pageUriComponentBuilder.pathSegment("groups");
					pageUriComponentBuilder.replaceQueryParam("pagesize", pagesize);
					pageUriComponentBuilder.replaceQueryParam("query", "group");
					
					pageCount = getPageCount(pageUriComponentBuilder, pagesize);
					
					//multi-thread the pages via futures
					for (int i = 1; i <= pageCount; i++) {
						pageUriComponentBuilder.replaceQueryParam("page", i);			
						URI pageUri = pageUriComponentBuilder.build().toUri();
						Future<Collection<String>> future = pageExecutorService.submit(getPageCallable(pageUri));
						pageFutures.put(pageUri.toString(), future);
						ThreadUtils.checkAndCallbackFutures(pageFutures, 100, pageCallback);
					}
					
					ThreadUtils.checkAndCallbackFutures(pageFutures, 0, pageCallback);
					ThreadUtils.checkAndCallbackFutures(accessionFutures, 0, accessionCallback);
	
					fileXmlWriter.write("</BioSamples>\n");
				}

		} finally {
			if (pageExecutorService != null) {
				pageExecutorService.shutdownNow();
			}
			if (accessionExecutorService != null) {
				accessionExecutorService.shutdownNow();
			}
		}
		long elapsed = System.nanoTime()-oldTime;
		log.info("Collected from "+rootUrl+" in "+(elapsed/1000000000l)+"s");
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
	
	public Callable<Collection<String>> getPageCallable(URI uri) {
		return new Callable<Collection<String>>(){

			@Override
			public Collection<String> call() throws Exception {
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
				
				Collection<String> accessions = new HashSet<>();
				
				for (Element element : XmlPathBuilder.of(root).elements("BioSample")) {
					String accession = element.attributeValue("id");
					accessions.add(accession);
				}
				for (Element element : XmlPathBuilder.of(root).elements("BioSampleGroup")) {
					String accession = element.attributeValue("id");
					accessions.add(accession);
				}
				
				return accessions;
			}
		};
	}
	

	public static class PageCallback implements ThreadUtils.Callback<Collection<String>> {

		private final ExecutorService accessionExecutorService;
		private final String rootUrl;
		private final RestTemplate restTemplate;
		private final AccessionCallback accessionCallback;
		
		private final Map<String, Future<String>> accessionFutures;
		private final Logger log = LoggerFactory.getLogger(getClass());
		
		public PageCallback(ExecutorService accessionExecutorService, 
				String rootUrl, RestTemplate restTemplate, 
				AccessionCallback accessionCallback, 
				Map<String, Future<String>> accessionFutures) {
			this.accessionExecutorService = accessionExecutorService;		
			this.rootUrl = rootUrl;
			this.restTemplate = restTemplate;
			this.accessionCallback = accessionCallback;
			this.accessionFutures = accessionFutures;
		}
		
		
		@Override
		public void call(Collection<String> accessions) {
			for (String accession : accessions) {
				UriComponentsBuilder accessionUriComponentBuilder = UriComponentsBuilder.fromUriString(rootUrl);
				if (accession.startsWith("SAMEG")) {
					accessionUriComponentBuilder.pathSegment("groups", accession);
				} else {
					accessionUriComponentBuilder.pathSegment("samples", accession);
				}
				URI accessionUri = accessionUriComponentBuilder.build().toUri();
				accessionFutures.put(accession, accessionExecutorService.submit(getAccessionCallable(accessionUri)));
				try {
					ThreadUtils.checkAndCallbackFutures(accessionFutures, 100, accessionCallback);
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
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
		
		public Callable<String> getAccessionCallable(URI uri) {
			return new Callable<String>(){
				@Override
				public String call() throws Exception {
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
					
					//strip the first two lines
					xmlString = xmlString.substring(xmlString.indexOf('\n')+1);
					xmlString = xmlString.substring(xmlString.indexOf('\n')+1);
					
					return xmlString;
				}
			};
		}

		public Callable<List<String>> getPageMembershipCallable(URI uri) {
			return new Callable<List<String>>() {
				@Override 
				public List<String> call() throws Exception {
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
					
					List<String> accessions = new ArrayList<>();
					
					List<String> path = UriComponentsBuilder.fromUri(uri).build().getPathSegments();
					accessions.add(path.get(path.size()-1));
					
					
					for (Element element : XmlPathBuilder.of(root).elements("BioSample")) {
						String accession = element.attributeValue("id");
						accessions.add(accession);
					}
					
					return accessions;
					
				}
			};
		}
	}
	

	public static class AccessionCallback implements ThreadUtils.Callback<String> {
		private final FileWriter fileWriter;
		
		public AccessionCallback(FileWriter fileWriter) {
			this.fileWriter = fileWriter;
		}

		@Override
		public void call(String xml) {
			try {
				fileWriter.write(xml);
				fileWriter.write("\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
	}
	
	private static class PageMembershipCallback implements ThreadUtils.Callback<List<String>> {

		private final CSVWriter fileCsvWriter;
		
		public PageMembershipCallback(CSVWriter fileCsvWriter ) {
			this.fileCsvWriter = fileCsvWriter;
		}
		
		@Override
		public void call(List<String> accessions) {
			if (accessions != null && accessions.size() > 1) {
				String[] toWrite = new String[accessions.size()];
				fileCsvWriter.writeNext(accessions.toArray(toWrite));
			}
		}
		
	}
}
