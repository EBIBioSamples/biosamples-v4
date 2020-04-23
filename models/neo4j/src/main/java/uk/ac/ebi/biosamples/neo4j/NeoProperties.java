package uk.ac.ebi.biosamples.neo4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NeoProperties {

	@Value("${biosamples.neo.url:bolt://localhost:7687}")
	private String neoUrl;

	@Value("${biosamples.neo.username:neo4j}")
	private String neoUsername;

	@Value("${biosamples.neo.password:neo5j}")
	private String neoPassword;

	public String getNeoUrl() {
		return neoUrl;
	}

	public String getNeoUsername() {
		return neoUsername;
	}

	public String getNeoPassword() {
		return neoPassword;
	}
}
