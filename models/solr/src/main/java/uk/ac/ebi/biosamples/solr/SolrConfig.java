package uk.ac.ebi.biosamples.solr;

import org.springframework.context.annotation.Configuration;

@Configuration
//do not use EnableSolrRepositories as it then disables spring boot config
public class SolrConfig {
	
	public static final String FETCHSOLRSAMPLEBYTEXT = "FETCHSOLRSAMPLEBYTEXT";
	public static final String GETFACETS = "GETFACETS";
	public static final String GETAUTOCOMPLETE = "GETAUTOCOMPLETE";
	
}
