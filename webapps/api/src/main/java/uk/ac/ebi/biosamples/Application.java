package uk.ac.ebi.biosamples;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.core.config.EnumTranslationConfiguration;
import org.springframework.data.rest.core.config.MetadataConfiguration;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Autowired
	private RepositoryRestConfiguration	repositoryRestConfiguration;
	
	@PostConstruct
	public void changeRepositoryDetectionStrategy() {
		repositoryRestConfiguration.setRepositoryDetectionStrategy(RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED);
	}
}
