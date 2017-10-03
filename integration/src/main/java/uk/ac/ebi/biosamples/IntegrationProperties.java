package uk.ac.ebi.biosamples;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class IntegrationProperties {

	
	@Value("${biosamples.client.uri:http://localhost:8081}")
	private URI biosampleSubmissionUri;
	
	@Value("${biosamples.submissionuri.sampletab:http://localhost:8082}")
	private URI biosampleSubmissionUriSampletab;

	@Value("${biosamples.legacyxml.uri:http://localhost:8083}")
	private URI biosampleLegaxyXmlUri;
	
	@Value("${biosamples.legacyapikey:#{null}}")
	private String legacyApiKey;

	public URI getBiosampleSubmissionUri() {
		return biosampleSubmissionUri;
	}

	public URI getBiosampleSubmissionUriSampleTab() {
		return biosampleSubmissionUriSampletab;
	}

	public URI getBiosampleLegaxyXmlUri() { 
		return biosampleLegaxyXmlUri; 
	}
	
	public String getLegacyApiKey() {
		return legacyApiKey;
	}
}
