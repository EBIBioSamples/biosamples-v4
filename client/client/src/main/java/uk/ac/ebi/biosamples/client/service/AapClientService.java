package uk.ac.ebi.biosamples.client.service;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

public class AapClientService {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	private final RestOperations restOperations;
	
	private final URI aapUri;
	private final String username;
	private final String password;
	
	private String jwt = null;
	
	public AapClientService(RestTemplateBuilder restTemplateBuilder, URI aapUri, String username, String password) {
		this.restOperations = restTemplateBuilder.build();
		this.aapUri = aapUri;
		this.username = username;
		this.password = password;
	}
	
	//TODO put some sort of cache/validation layer over this
	public String getJwt() {		
		String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII")) );
        String authHeader = "Basic " + new String( encodedAuth );		
		
		ResponseEntity<String> response = restOperations.exchange(RequestEntity.get(aapUri)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.accept(MediaType.TEXT_PLAIN)
				.build(), String.class);
		
		String jwt = response.getBody();
		log.info("jwt = "+jwt);
		return jwt;
	}
}
