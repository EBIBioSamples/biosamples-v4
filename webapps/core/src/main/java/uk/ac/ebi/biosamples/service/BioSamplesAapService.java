package uk.ac.ebi.biosamples.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import uk.ac.ebi.tsc.aap.client.model.Domain;

@Service
public class BioSamplesAapService {
	
	public Set<String> getDomains(Authentication authentication) {
		if (authentication == null) {
			return Collections.emptySet();
		}
		Set<String> domains = new HashSet<>();
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (authority instanceof Domain) {
				Domain domain = (Domain) authority;
				domains.add(domain.getDomainReference());
			}
		}
		return domains;
	}
}
