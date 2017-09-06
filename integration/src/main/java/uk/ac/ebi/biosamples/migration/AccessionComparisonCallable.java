package uk.ac.ebi.biosamples.migration;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonFormatter;
import org.xmlunit.diff.DefaultComparisonFormatter;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.builder.Input;
import org.xmlunit.input.CommentLessSource;
import org.xmlunit.input.WhitespaceNormalizedSource;

class AccessionComparisonCallable implements Callable<Void> {
	private final RestTemplate restTemplate;
	private final String oldUrl;
	private final String newUrl;
	private final Queue<String> bothQueue;
	private final AtomicBoolean bothFlag;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public AccessionComparisonCallable(RestTemplate restTemplate, String oldUrl, String newUrl, 
			Queue<String> bothQueue, AtomicBoolean bothFlag) {
		this.restTemplate = restTemplate;
		this.oldUrl = oldUrl;
		this.newUrl = newUrl;
		this.bothQueue = bothQueue;
		this.bothFlag = bothFlag;
	}
	
	@Override
	public Void call() throws Exception {
		log.info("Started AccessionComparisonCallable.call(");
		
		UriComponentsBuilder oldUriComponentBuilder = UriComponentsBuilder.fromUriString(oldUrl);
		UriComponentsBuilder newUriComponentBuilder = UriComponentsBuilder.fromUriString(newUrl);
		
		ComparisonFormatter comparisonFormatter = new DefaultComparisonFormatter();
		
		while (!bothFlag.get() || !bothQueue.isEmpty()) {
			String accession = bothQueue.poll();
			if (accession != null) {
				log.info("Comparing accession "+accession);
				URI oldUri = oldUriComponentBuilder.cloneBuilder().pathSegment(accession).build().toUri();
				URI newUri = newUriComponentBuilder.cloneBuilder().pathSegment(accession).build().toUri();
				String oldDocument = getDocument(oldUri);
				String newDocument = getDocument(newUri);
								
				Diff diff = DiffBuilder.compare(new WhitespaceNormalizedSource(new CommentLessSource(Input.fromString(oldDocument).build())))
						.withTest(new WhitespaceNormalizedSource(new CommentLessSource(Input.fromString(newDocument).build())))
						.build();
				if (diff.hasDifferences()) {
					List<Difference> differences = new ArrayList<>();
					for (Difference difference : diff.getDifferences()) {
						differences.add(difference);
					}
					
					log.info("Found "+differences.size()+" on "+accession);
					
					
					for (Difference difference : differences.subList(0, 5)) {						
						log.info(comparisonFormatter.getDescription(difference.getComparison()));
					}
					
				}
			}
		}
		log.info("Finished AccessionComparisonCallable.call(");
		return null;
	}
	
	public String getDocument(URI uri) throws DocumentException {
		log.info("Getting "+uri);
		ResponseEntity<String> response;
		try {
			response = restTemplate.getForEntity(uri, String.class);
		} catch (RestClientException e) {
			log.error("Problem accessing "+uri, e);
			throw e;
		}
		String xmlString = response.getBody();
		System.out.println(xmlString);
		//SAXReader reader = new SAXReader();
		//Document xml = reader.read(new StringReader(xmlString));	
		return xmlString;
	}
	
}