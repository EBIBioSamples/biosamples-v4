package uk.ac.ebi.biosamples.accession;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AccessionConfig {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Bean("accessionDataSource")
	@ConfigurationProperties(prefix="spring.datasource.accession")
	public DataSource getAccessionDataSource() {
	    return DataSourceBuilder.create().build();
	}
	
	@Bean("accessionJdbcTemplate")
	public JdbcTemplate getAccessionJdbcTemplate(@Qualifier("accessionDataSource") DataSource accessionDataSource) {
	    return new JdbcTemplate(accessionDataSource);
	}
	
}
