package uk.ac.ebi.biosamples.solr;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.CaffeineSpec;

@Configuration
//do not use EnableSolrRepositories as it then disables spring boot config
public class SolrConfig {
	
	public static final String FETCHSOLRSAMPLEBYTEXT = "FETCHSOLRSAMPLEBYTEXT";
	public static final String GETFACETS = "GETFACETS";
	public static final String GETAUTOCOMPLETE = "GETAUTOCOMPLETE";
	
}
