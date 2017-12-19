package uk.ac.ebi.biosamples.migration;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.common.collect.Sets;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Organization;
import uk.ac.ebi.biosamples.model.Publication;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.XmlGroupToSampleConverter;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class XmlCompareCallable implements Callable<Void> {
	
	private final String accession;
	private final String oldUrl;
	private final String newUrl;
	private final XmlSampleToSampleConverter xmlSampleToSampleConverter;
	private final XmlGroupToSampleConverter xmlGroupToSampleConverter;
	private final RestTemplate restTemplate;
	private final Comparer comparer = new Comparer();
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public XmlCompareCallable(String accession, String oldUrl, String newUrl, 
			XmlSampleToSampleConverter xmlSampleToSampleConverter, XmlGroupToSampleConverter xmlGroupToSampleConverter,
			RestTemplate restTemplate) {
		this.accession = accession;
		this.oldUrl = oldUrl;
		this.newUrl = newUrl;
		this.xmlSampleToSampleConverter = xmlSampleToSampleConverter;
		this.xmlGroupToSampleConverter = xmlGroupToSampleConverter;
		this.restTemplate = restTemplate;
	}

	@Override
	public Void call() throws Exception {
		compare(accession);
		return null;
	}

	public void compare(String accession) {
		log.info("Comparing accession " + accession);

		UriComponentsBuilder oldUriComponentBuilder = UriComponentsBuilder.fromUriString(oldUrl);
		UriComponentsBuilder newUriComponentBuilder = UriComponentsBuilder.fromUriString(newUrl);
		if (accession.startsWith("SAMEG")) {
			oldUriComponentBuilder = oldUriComponentBuilder.pathSegment("groups");
			newUriComponentBuilder = newUriComponentBuilder.pathSegment("groups");
		} else {
			oldUriComponentBuilder = oldUriComponentBuilder.pathSegment("samples");	
			newUriComponentBuilder = newUriComponentBuilder.pathSegment("samples");			
		}
		
		URI oldUri = oldUriComponentBuilder.pathSegment(accession).build().toUri();
		URI newUri = newUriComponentBuilder.pathSegment(accession).build().toUri();
		String oldDocument = getDocument(oldUri);
		String newDocument = getDocument(newUri);


		SAXReader saxReader = new SAXReader();
		org.dom4j.Document doc;
		try {
			doc = saxReader.read(new StringReader(oldDocument));
		} catch (DocumentException e) {
			throw new HttpMessageNotReadableException("error parsing xml", e);
		}
		Sample oldSample = null;
		if (accession.startsWith("SAMEG")) {
			oldSample = xmlGroupToSampleConverter.convert(doc.getRootElement());
			addGroupMembership(oldSample, oldUriComponentBuilder, accession);
		} else {
			oldSample = xmlSampleToSampleConverter.convert(doc.getRootElement());
		}
		
		
		try {
			doc = saxReader.read(new StringReader(newDocument));
		} catch (DocumentException e) {
			throw new HttpMessageNotReadableException("error parsing xml", e);
		}
		Sample newSample = null;
		if (accession.startsWith("SAMEG")) {
			newSample = xmlGroupToSampleConverter.convert(doc.getRootElement());
			addGroupMembership(newSample, newUriComponentBuilder, accession);			
		} else {
			newSample = xmlSampleToSampleConverter.convert(doc.getRootElement());
		}
		
		comparer.compare(accession, oldSample, newSample);		
	}
	
	public String getDocument(URI uri) {
		long startTime = System.nanoTime();
		//log.info("Getting " + uri);
		ResponseEntity<String> response;
		try {
			response = restTemplate.getForEntity(uri, String.class);
		} catch (RestClientException e) {
			log.error("Problem accessing " + uri, e);
			throw e;
		}
		String xmlString = response.getBody();
		long endTime = System.nanoTime();
		long interval = (endTime-startTime)/1000000l;
		log.info("Got "+uri+" in "+interval+"ms");
		
		return xmlString;
	}
	
	private void addGroupMembership(Sample sample, UriComponentsBuilder uriComponentsBuilder, String accession) {
		SortedSet<String> members = getGroupMembership(uriComponentsBuilder, accession);
		for (String member : members) {
			sample.getRelationships().add(Relationship.build(sample.getAccession(), "has member", member));
		}
	}
	
	private SortedSet<String> getGroupMembership(UriComponentsBuilder uriComponentsBuilder, String accession) {
		int total = -1;
		int to = -1;
		int page = 1;
		
		SortedSet<String> members = new TreeSet<>();
		
		while (total < 0 || to < total) {
			URI uri = uriComponentsBuilder.cloneBuilder().pathSegment("groupsamples", accession)
					.replaceQueryParam("pagesize", 1000)
					.replaceQueryParam("page", page)
					.replaceQueryParam("query", "")
					.build().toUri();	

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
			org.dom4j.Document xml = null;
			try {
				xml = reader.read(new StringReader(xmlString));
			} catch (DocumentException e) {
				throw new RuntimeException(e);
			}
			Element root = xml.getRootElement();

			//add members to set
			for (Element element : XmlPathBuilder.of(root).elements("BioSample")) {
				members.add( element.attributeValue("id"));
			}
			//get the total and to values
			total = Integer.valueOf(XmlPathBuilder.of(root).path("SummaryInfo", "Total").text());
			to = Integer.valueOf(XmlPathBuilder.of(root).path("SummaryInfo", "To").text());
		}
		
		return members;
	}


	
}