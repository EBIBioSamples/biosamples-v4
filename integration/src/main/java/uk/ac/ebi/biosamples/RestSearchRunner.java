package uk.ac.ebi.biosamples;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Component
public class RestSearchRunner implements ApplicationRunner, ExitCodeGenerator, Ordered {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private IntegrationProperties integrationProperties;

	@Autowired
	private RestOperations restTemplate;
	
	private int exitCode = 1;

	@Override
	public void run(ApplicationArguments args) throws Exception {

		log.info("Starting RestSearchRunner");
		
		//Sample sampleTest1 = getSampleTest1();

		if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 1) {
		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 2) {

			URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri()).pathSegment("samples").build().toUri();

			log.info("GETting from "+uri);
			RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
			ResponseEntity<Resources<Resource<Sample>>> response = restTemplate.exchange(request, new ParameterizedTypeReference<Resources<Resource<Sample>>>(){});
			//check that there is at least one sample returned
			//if there are zero, then probably nothing was indexed
			if (response.getBody().getContent().size() <= 0) {
				throw new RuntimeException("No search results found!");
			}
		}
		
		//if we got here without throwing, then we finished sucessfully
		exitCode = 0;
		log.info("Finished RestSearchRunner");
	}

	private Sample getSampleTest1() throws URISyntaxException {
		String name = "Test Sample";
		String accession = "TEST1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		attributes.add(Attribute.build("age", "3", null, "year"));
		attributes.add(Attribute.build("organism part", "lung", null, null));
		attributes.add(Attribute.build("organism part", "heart", null, null));

		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("TEST1", "derived from", "TEST2"));
		
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		externalReferences.add(ExternalReference.build("http://www.google.com"));

		return Sample.build(name, accession, release, update, attributes, relationships, externalReferences);
	}

	@Override
	public int getOrder() {
		return 2;
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
