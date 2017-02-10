package uk.ac.ebi.biosamples;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IntegrationProperties {

	
	@Value("${biosamples.submissionuri:http://localhost:8081}")
	private URI biosampleSubmissionURI;
	
	public URI getBiosampleSubmissionURI() {
		return biosampleSubmissionURI;
	}
}
