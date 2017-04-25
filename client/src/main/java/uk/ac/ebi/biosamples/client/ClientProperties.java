package uk.ac.ebi.biosamples.client;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientProperties {
	
	@Value("${biosamples.client.uri:http://localhost:8081}")
	private URI biosamplesClientUri;

	public URI getBiosamplesClientUri() {
		return biosamplesClientUri;
	}
}
