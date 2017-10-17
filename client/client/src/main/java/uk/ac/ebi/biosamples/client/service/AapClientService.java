package uk.ac.ebi.biosamples.client.service;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class AapClientService {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	private final RestOperations restOperations;
	
	private final URI aapUri;
	private final String username;
	private final String password;
	
	private Optional<String> jwt = Optional.empty();
	private Optional<Date> expiry = Optional.empty();
	
	public AapClientService(RestTemplateBuilder restTemplateBuilder, URI aapUri, String username, String password) {
		this.restOperations = restTemplateBuilder.build();
		this.aapUri = aapUri;
		this.username = username;
		this.password = password;
	}
	
	public synchronized String getJwt() {	
		
		if (username == null || username.trim().length() == 0
				|| password == null || password.trim().length() == 0) {
			return null;
		}
		
		//TODO refresh token when less than 5 minutes left, rather than when expired
		if (!jwt.isPresent() || (expiry.isPresent() && expiry.get().before(new Date()))) {
		
			String auth = username + ":" + password;
	        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII")) );
	        String authHeader = "Basic " + new String( encodedAuth );		
			
	        RequestEntity<?> request = RequestEntity.get(aapUri)
					.header(HttpHeaders.AUTHORIZATION, authHeader)
					//.accept(MediaType.TEXT_PLAIN)
					.build();
	        
			ResponseEntity<String> response = restOperations.exchange(request, String.class);
			
			jwt = Optional.of(response.getBody());
			
			try {
			    DecodedJWT decodedJwt = JWT.decode(jwt.get());
			    expiry = Optional.of(decodedJwt.getExpiresAt());
			} catch (JWTDecodeException e){
			    //Invalid token
				throw new RuntimeException(e);
			}

		log.info("jwt = "+jwt);
		}
		
		return jwt.get();
	}
}
