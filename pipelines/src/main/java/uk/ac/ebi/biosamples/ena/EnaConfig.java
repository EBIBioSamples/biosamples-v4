package uk.ac.ebi.biosamples.ena;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EnaConfig {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Bean("eraDataSource")
	@ConfigurationProperties(prefix="spring.datasource.erapro")
	public DataSource getEraDataSource() {
	    return DataSourceBuilder.create().build();
	}
	@Bean("eraJdbcTemplate")
	public JdbcTemplate getEraJdbcTemplate(@Qualifier("eraDataSource") DataSource eraDataSource) {
	    return new JdbcTemplate(eraDataSource);
	}
	
}
