package uk.ac.ebi.biosamples.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.data.solr.server.support.HttpSolrClientFactory;
import org.springframework.data.solr.server.support.MulticoreSolrClientFactory;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

@Configuration
//do not use EnableSolrRepositories as it then disables spring boot config
public class SolrConfig {

	@Bean
	public MulticoreSolrClientFactory MulticoreSolrClientFactory(SolrClient solrClient) {
		return new MulticoreSolrClientFactory(solrClient);
	}
	
	@Bean("solrOperationsSample")
	public SolrOperations solrOperations(MulticoreSolrClientFactory multicoreSolrClientFactory, SolrConverter solrConverter) {
		SolrClient solrClient = multicoreSolrClientFactory.getSolrClient(SolrSample.class);
		
		SolrClientFactory solrClientFactory;
		
		if (HttpSolrClient.class.isAssignableFrom(solrClient.getClass())) {
			solrClientFactory = new HttpSolrClientFactory(solrClient);
		} else {
			throw new IllegalArgumentException("Non HttpSolrClient in use");
		}
		
		SolrTemplate solrTemplate = new SolrTemplate(solrClientFactory, solrConverter);
		//this needs to be called after construction to fully initialize
		solrTemplate.afterPropertiesSet();	
		
		return solrTemplate;
	}

    @Value("${spring.data.solr.host}")
    String solrHost;

    //workaround for 1.5.x problems with multicore solr
    @Bean
    @Primary
    public SolrTemplate solrTemplate(){
        CustomSolrTemplate template = new CustomSolrTemplate(new HttpSolrClient(solrHost));
        return template;
    }
}
