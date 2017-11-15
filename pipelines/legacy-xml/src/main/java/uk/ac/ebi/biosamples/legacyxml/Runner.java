package uk.ac.ebi.biosamples.legacyxml;

import java.io.StringReader;
import java.net.URI;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

//@Component
public class Runner implements ApplicationRunner {

	@Value("${biosamples.pipelines.legacyxml.url:http://www.ebi.ac.uk/biosamples/xml/samples}")
	private String rootUrl;	
	
	private final int pagesize = 1000;
	private final RestTemplate restTemplate;
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final XmlSampleToSampleConverter xmlToSampleConverter;

	private final BioSamplesClient client;
	
	
	public Runner(RestTemplateBuilder restTemplateBuilder, XmlSampleToSampleConverter xmlToSampleConverter, BioSamplesClient client) {
		this.restTemplate = restTemplateBuilder.build();
		this.xmlToSampleConverter = xmlToSampleConverter;
		this.client = client;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		UriComponentsBuilder uriComponentBuilder = UriComponentsBuilder.fromUriString(rootUrl);
		uriComponentBuilder.replaceQueryParam("pagesize", pagesize);
		uriComponentBuilder.replaceQueryParam("query", "");
		
		int pageCount = getPageCount(uriComponentBuilder, pagesize);

		for (int i = 1; i <= pageCount; i++) {
			uriComponentBuilder.replaceQueryParam("page", i);			
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
			
			for (Element element : XmlPathBuilder.of(root).elements("BioSample")) {
				String accession = element.attributeValue("id"); 
				//only handle sample accessions for now
				if (!accession.startsWith("SAMEG")) {

					UriComponentsBuilder xmlUriComponentBuilder = UriComponentsBuilder.fromUriString(rootUrl);

					URI oldUri = xmlUriComponentBuilder.cloneBuilder().pathSegment(accession).build().toUri();
					String oldDocument = getDocument(oldUri);

					SAXReader saxReader = new SAXReader();
					org.dom4j.Document doc;
					try {
						doc = saxReader.read(new StringReader(oldDocument));
					} catch (DocumentException e) {
						throw new HttpMessageNotReadableException("error parsing xml", e);
					}
					Sample sample = xmlToSampleConverter.convert(doc);
					
					log.info("persisting "+sample);
					client.persistSample(sample);
				}
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
