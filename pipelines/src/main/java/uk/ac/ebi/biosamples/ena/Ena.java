package uk.ac.ebi.biosamples.ena;

import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.models.Sample;

@Component
public class Ena implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private EraProDao eraprodao;

	@Autowired
	private PipelinesProperties pipelinesProperties;
	
	@Autowired
	private RestTemplate restTemplate;

	
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		// only do things if we are told to
		if (!args.containsOption("ena")) {
			log.info("skipping ena");
			return;
		}

		log.info("Processing ENA pipeline...");

		// date format is YYYY-mm-dd
		LocalDate fromDate = null;
		if (args.getOptionNames().contains("from")) {
			fromDate = LocalDate.parse(args.getOptionValues("from").iterator().next(),
					DateTimeFormatter.ISO_LOCAL_DATE);
		} else {
			fromDate = LocalDate.parse("1000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
		}
		LocalDate toDate = null;
		if (args.getOptionNames().contains("until")) {
			toDate = LocalDate.parse(args.getOptionValues("until").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
		} else {
			toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
		}
		
		for (String sampleAccession : eraprodao.getSamples(fromDate, toDate)) {
			log.info("HANDLING " + sampleAccession);
			
			
			//https://www.ebi.ac.uk/ena/data/view/SAMEA1317921&display=xml works
			//https://www.ebi.ac.uk/ena/data/view/SAMEA1317921?display=xml is a more correct URL, but doesn't work
			
			URI uri = UriComponentsBuilder.newInstance().scheme("http").host("www.ebi.ac.uk").pathSegment("ena","data","view",sampleAccession+"&display=xml").build().toUri();
			log.info("looking at "+uri);
			ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
			if (!response.getStatusCode().is2xxSuccessful()) {
				log.error("Non-2xx status code for "+sampleAccession);
				continue;
			}
			
			String xmlString = response.getBody();
			System.out.println(xmlString);
			SAXReader reader = new SAXReader();
			Document xml = reader.read(new StringReader(xmlString));

			
			log.info("HANDLED " + sampleAccession);
		}
	}
	
}
