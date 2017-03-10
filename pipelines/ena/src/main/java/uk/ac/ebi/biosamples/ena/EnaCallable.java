package uk.ac.ebi.biosamples.ena;

import java.io.StringReader;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.service.SubmissionService;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Component
//this makes sure that we have a different instance wherever it is used
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EnaCallable implements Callable<Void> {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private SubmissionService submissionService;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private EnaElementConverter enaElementConverter;

	@Autowired
	private EraProDao eraProDao;

	private final String sampleAccession;
	
	public EnaCallable(String sampleAccession) {
		this.sampleAccession = sampleAccession;
	}
	
	@Override
	public Void call() throws Exception {
		log.info("HANDLING " + sampleAccession);
		
		
		//https://www.ebi.ac.uk/ena/data/view/SAMEA1317921&display=xml works
		//https://www.ebi.ac.uk/ena/data/view/SAMEA1317921?display=xml is a more correct URL, but doesn't work
		
		URI uri = UriComponentsBuilder.newInstance().scheme("http").host("www.ebi.ac.uk").pathSegment("ena","data","view",sampleAccession+"&display=xml").build().toUri();
		log.trace("looking at "+uri);
		ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
		if (!response.getStatusCode().is2xxSuccessful()) {
			log.error("Non-2xx status code for "+sampleAccession);
			return null;
		}
		
		String xmlString = response.getBody();
		//System.out.println(xmlString);
		SAXReader reader = new SAXReader();
		Document xml = reader.read(new StringReader(xmlString));
		Element root = xml.getRootElement();
		//check that we got some content
		if (XmlPathBuilder.of(root).path("SAMPLE").exists()) {
			Sample sample = enaElementConverter.convert(root);
			//add dates from database
			LocalDateTime release = eraProDao.getReleaseDateTime(sampleAccession);
			LocalDateTime update = eraProDao.getUpdateDateTime(sampleAccession);
			
			sample = Sample.build(sample.getName(), sampleAccession, release, update, sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());
			submissionService.submit(sample);
		} else {
			log.warn("Unable to find SAMPLE element for "+sampleAccession);
		}
		log.info("HANDLED " + sampleAccession);
		return null;
	}

}
