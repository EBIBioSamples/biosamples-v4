package uk.ac.ebi.biosamples;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	

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
