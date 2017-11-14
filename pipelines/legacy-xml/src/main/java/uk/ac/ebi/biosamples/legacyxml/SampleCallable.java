package uk.ac.ebi.biosamples.legacyxml;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

public class SampleCallable implements Callable<Void> {	
	
	private final RestTemplate restTemplate;
	private final String xmlUrl;
	private final Queue<String> queue;
	private final AtomicBoolean flag;
	private final XmlSampleToSampleConverter xmlToSampleConverter;
	private final BioSamplesClient client;
	private final ExecutorService executorService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final Map<String, Future<Void>> futures = new HashMap<>();
	

	public SampleCallable (RestTemplate restTemplate, String xmlUrl, 
			Queue<String> queue, AtomicBoolean flag, 
			XmlSampleToSampleConverter xmlToSampleConverter, BioSamplesClient client) {
		this.restTemplate = restTemplate;
		this.xmlUrl = xmlUrl;
		this.queue = queue;
		this.flag = flag;
		this.xmlToSampleConverter = xmlToSampleConverter;
		this.client = client;
		this.executorService = Executors.newFixedThreadPool(32);
	}
	
	@PreDestroy
	public void shutdown() {
		this.executorService.shutdownNow();
	}
	
	@Override
	public Void call() throws Exception {
		while (!flag.get() || !queue.isEmpty()) {
			String accession = queue.poll();
			if (accession != null) {
				handle(accession);
			} else {
				Thread.sleep(100);
			}
			ThreadUtils.checkFutures(futures, queue.size());
		}
		ThreadUtils.checkFutures(futures, 0);
		log.info("Finished AccessionComparisonCallable.call(");
		return null;
	}

	private void handle(String accession) {
		
		log.info("Handling "+accession);
		
		futures.put(accession, executorService.submit(
				new AccessionCallable(accession, restTemplate, xmlUrl, xmlToSampleConverter, client)));
		
		
		
		//Future<Resource<Sample>> future = client.persistSampleResourceAsync(sample, false);
		//futures.put(accession, future);
	}

	private static class AccessionCallable implements Callable<Void> {
		
		private final String accession;
		private final RestTemplate restTemplate;
		private final String xmlUrl;
		private final XmlSampleToSampleConverter xmlToSampleConverter;
		private final BioSamplesClient client;
		
		private final Logger log = LoggerFactory.getLogger(getClass());
		
		public AccessionCallable(String accession, RestTemplate restTemplate, String xmlUrl, XmlSampleToSampleConverter xmlToSampleConverter, BioSamplesClient client) {
			this.accession = accession;
			this.restTemplate = restTemplate;
			this.xmlUrl = xmlUrl;
			this.xmlToSampleConverter = xmlToSampleConverter;
			this.client = client;
		}

		@Override
		public Void call() throws Exception {
			UriComponentsBuilder xmlUriComponentBuilder = UriComponentsBuilder.fromUriString(xmlUrl);

			URI oldUri = xmlUriComponentBuilder.cloneBuilder().pathSegment(accession).build().toUri();
			String oldDocument = getDocument(oldUri);

			SAXReader saxReader = null;
			StringReader stringReader = null;
			org.dom4j.Document doc;
			
			saxReader = new SAXReader();
			try {
				stringReader = new StringReader(oldDocument);
				doc = saxReader.read(stringReader);
			} catch (DocumentException e) {
				throw new HttpMessageNotReadableException("error parsing xml", e);
			} finally {
				if (stringReader != null) {
					stringReader.close();
				}
			}
			Sample sample = xmlToSampleConverter.convert(doc);
			//need to specify domain
			sample = Sample.build(sample.getName(), sample.getAccession(), "foo", sample.getRelease(), sample.getUpdate(), sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());
			
			log.info("persisting "+sample);
			client.persistSampleResource(sample, false);
			return null;
		}

		public String getDocument(URI uri) {
			log.info("Getting " + uri);
			ResponseEntity<String> response;
			try {
				response = restTemplate.getForEntity(uri, String.class);
			} catch (RestClientException e) {
				log.error("Problem accessing " + uri, e);
				throw e;
			}
			String xmlString = response.getBody();
			return xmlString;
		}
		
	}

}
