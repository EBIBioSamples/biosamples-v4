package uk.ac.ebi.biosamples.ena;

import java.io.StringReader;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class EnaCallable implements Callable<Void> {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final String sampleAccession;
	private final BioSamplesClient bioSamplesClient;
	private final RestTemplate restTemplate;
	private final EnaElementConverter enaElementConverter;
	private final EraProDao eraProDao;
	
	public static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<String>();

	//TODO do this properly
	private final String domain = "DUMMY-DOMAIN";
	
	public EnaCallable(String sampleAccession, BioSamplesClient bioSamplesClient, RestTemplate restTemplate,
			EnaElementConverter enaElementConverter, EraProDao eraProDao) {
		this.sampleAccession = sampleAccession;
		this.bioSamplesClient = bioSamplesClient;
		this.restTemplate = restTemplate;
		this.enaElementConverter = enaElementConverter;
		this.eraProDao = eraProDao;
	}

	@Override
	public Void call() throws Exception {
		log.info("HANDLING " + sampleAccession);
		
		try {

			// https://www.ebi.ac.uk/ena/data/view/SAMEA1317921&display=xml works
			// https://www.ebi.ac.uk/ena/data/view/SAMEA1317921?display=xml is a
			// more correct URL, but doesn't work	
			URI uri = UriComponentsBuilder.newInstance().scheme("http").host("www.ebi.ac.uk")
					.pathSegment("ena", "data", "view", sampleAccession + "&display=xml").build().toUri();
			
			log.trace("looking at " + uri);
			ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
			if (!response.getStatusCode().is2xxSuccessful()) {
				log.error("Non-2xx status code for " + sampleAccession);
				return null;
			}
	
			String xmlString = response.getBody();
			// System.out.println(xmlString);
			SAXReader reader = new SAXReader();
			Document xml = reader.read(new StringReader(xmlString));
			Element root = xml.getRootElement();
			// check that we got some content
			if (XmlPathBuilder.of(root).path("SAMPLE").exists()) {
				Sample sample = enaElementConverter.convert(root);
				// add dates from database
				Instant release = eraProDao.getReleaseDateTime(sampleAccession);
				Instant update = eraProDao.getUpdateDateTime(sampleAccession);

				sample = Sample.build(sample.getName(), sampleAccession, domain, release, update, sample.getCharacteristics(),
						sample.getRelationships(), sample.getExternalReferences());
				bioSamplesClient.persistSampleResource(sample);
			} else {
				log.warn("Unable to find SAMPLE element for " + sampleAccession);
			}
		} catch (HttpServerErrorException e) {
			log.error("Request failed with : "+e.getResponseBodyAsString());
			failedQueue.add(sampleAccession);
		} catch (Exception e) {
			log.info("Adding "+sampleAccession+" to fail queue");
			failedQueue.add(sampleAccession);
		}
		log.info("HANDLED " + sampleAccession);
		return null;
	}

}
