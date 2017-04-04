package uk.ac.ebi.biosamples.neo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableNeo4jRepositories(basePackageClasses = NeoConfig.class)
@EnableTransactionManagement
public class NeoConfig {

	private Logger log = LoggerFactory.getLogger(getClass());

	public NeoConfig() {
	};

}
