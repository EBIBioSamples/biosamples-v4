package uk.ac.ebi.biosamples.client;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientProperties {
	
	@Value("${biosamples.client.uri:http://localhost:8081}")
	private URI biosamplesClientUri;
	
	@Value("${biosamples.client.aap.uri:https://explore.api.aap.tsi.ebi.ac.uk/auth}")
	private URI biosamplesClientAapUri;
	
	@Value("${biosamples.client.aap.username}")
	private String biosamplesClientAapUsername;
	
	@Value("${biosamples.client.aap.password}")
	private String biosamplesClientAapPassword;
	
	public URI getBiosamplesClientUri() {
		return biosamplesClientUri;
	}
	
	public URI getBiosamplesClientAapUri() {
		return biosamplesClientAapUri;
	}
	
	public String getBiosamplesClientAapUsername() {
		return biosamplesClientAapUsername;
	}
	
	public String getBiosamplesClientAapPassword() {
		return biosamplesClientAapPassword;
	}
}
