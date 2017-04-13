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
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Component
public class RestAutocompleteRunner implements ApplicationRunner, ExitCodeGenerator, Ordered {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private IntegrationProperties integrationProperties;

	@Autowired
	private RestOperations restTemplate;
	
	private int exitCode = 1;

	@Override
	public void run(ApplicationArguments args) throws Exception {

		log.info("Starting RestAutocompleteRunner");

		if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 1) {
		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 2) {

			URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri())
					.pathSegment("samples").pathSegment("autocomplete").build().toUri();

			log.info("GETting from "+uri);
			RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
			ResponseEntity<Autocomplete> response = restTemplate.exchange(request, new ParameterizedTypeReference<Autocomplete>(){});
			//check that there is at least one sample returned
			//if there are zero, then probably nothing was indexed
			if (response.getBody().getSuggestions().size() <= 0) {
				throw new RuntimeException("No autocomplete suggestions found!");
			}
		}
		
		//if we got here without throwing, then we finished sucessfully
		exitCode = 0;
		log.info("Finished RestSearchRunner");
	}


	@Override
	public int getOrder() {
		return 4;
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
