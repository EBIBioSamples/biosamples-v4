package uk.ac.ebi.biosamples;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.template.Neo4jTemplate;

@Configuration
public class NeoConfig {


	private Logger log = LoggerFactory.getLogger(getClass());

	@PostConstruct
	public void createIndexes(Neo4jTemplate neoTemplate) {
		log.info("Creating uniqueness constraint");
		neoTemplate.execute("CREATE CONSTRAINT ON (sample:Sample) ASSERT sample.accession IS UNIQUE");
	}
}
