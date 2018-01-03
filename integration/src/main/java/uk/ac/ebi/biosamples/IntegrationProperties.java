package uk.ac.ebi.biosamples;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IntegrationProperties {
	
	@Value("${biosamples.submissionuri.sampletab:http://localhost:8082}")
	private URI biosampleSubmissionUriSampletab;

	@Value("${biosamples.legacyxml.uri:http://localhost:8083}")
	private URI biosamplesLegacyXMLUri;

	@Value("${biosamples.legacyjson.uri:http://localhost:8084}")
	private URI biosamplesLegacyJSONUri;
	
	@Value("${biosamples.legacyapikey:#{null}}")
	private String legacyApiKey;

	public URI getBiosampleSubmissionUriSampleTab() {
		return biosampleSubmissionUriSampletab;
	}

	public URI getBiosamplesLegacyXMLUri() {
		return biosamplesLegacyXMLUri;
	}
	
	public String getLegacyApiKey() {
		return legacyApiKey;
	}

	public URI getBiosamplesLegacyJSONUri() { return biosamplesLegacyJSONUri; }
}
