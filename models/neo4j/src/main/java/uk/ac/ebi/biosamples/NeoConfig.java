package uk.ac.ebi.biosamples;

import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;

@Configuration
public class NeoConfig {


	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private Neo4jOperations neoTemplate;

	public NeoConfig(){};
	
	
	@PostConstruct
	public void createIndexes() {
		log.info("Creating uniqueness constraint");
		neoTemplate.query("CREATE CONSTRAINT ON (sample:Sample) ASSERT sample.accession IS UNIQUE", new HashMap<>());
	}
}
