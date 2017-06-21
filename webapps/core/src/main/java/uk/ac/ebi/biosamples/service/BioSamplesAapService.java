package uk.ac.ebi.biosamples.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.security.UserAuthentication;

@Service
public class BioSamplesAapService {

	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	public Set<String> getDomains() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		log.info("authentication = "+authentication);
		
		
		UserAuthentication userAuthentication = (UserAuthentication) authentication;
				
		log.info("userAuthentication = "+userAuthentication.getName());
		log.info("userAuthentication = "+userAuthentication.getAuthorities());
		log.info("userAuthentication = "+userAuthentication.getPrincipal());
		log.info("userAuthentication = "+userAuthentication.getCredentials());
		
		if (authentication == null) {
			return Collections.emptySet();
		}
		Set<String> domains = new HashSet<>();
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (authority instanceof Domain) {
				log.info("Found domain "+authority);
				Domain domain = (Domain) authority;

				log.info("domain.getDomainName() = "+domain.getDomainName());
				log.info("domain.getDomainReference() = "+domain.getDomainReference());
				
				//NOTE this should use reference, but that is not populated in the tokens at the moment
				//domains.add(domain.getDomainReference());
				domains.add(domain.getDomainName());
			} else {
				log.info("Found non-domain "+authority);
			}
		}
		return domains;
	}
}
