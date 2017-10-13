package uk.ac.ebi.biosamples;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleToXmlConverter;
import uk.ac.ebi.biosamples.service.XmlSampleHttpMessageConverter;
import uk.ac.ebi.biosamples.service.XmlToSampleConverter;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Bean
	public HttpMessageConverter<Sample> getXmlSampleHttpMessageConverter(SampleToXmlConverter sampleToXmlConverter, XmlToSampleConverter xmlToSampleConverter) {
		return new XmlSampleHttpMessageConverter(sampleToXmlConverter, xmlToSampleConverter);
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
