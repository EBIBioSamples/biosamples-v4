package uk.ac.ebi.biosamples;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

@EnableSolrRepositories(multicoreSupport = true)
@SpringBootApplication
public class Application {	

	
	@Bean
	public SolrOperations getSamplesSolrOperations(SolrClient solrClient) {
		return new SolrTemplate(solrClient, "samples");
	}
	
	
	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(Application.class, args)));
	}
}
