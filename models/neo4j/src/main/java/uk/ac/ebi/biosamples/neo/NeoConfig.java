package uk.ac.ebi.biosamples.neo;

import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@Configuration
@EnableNeo4jRepositories(basePackageClasses=NeoConfig.class)
public class NeoConfig {


	private Logger log = LoggerFactory.getLogger(getClass());

	public NeoConfig(){};
	
	}
