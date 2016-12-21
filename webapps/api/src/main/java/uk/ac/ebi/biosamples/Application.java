package uk.ac.ebi.biosamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.handler.MappedInterceptor;

import uk.ac.ebi.biosamples.mongo.SampleResourceProcessor;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.xml.XmlSampleHttpMessageConverter;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Bean
	public MappedInterceptor getCacheHeaderMappedInterceptor() {
	    return new MappedInterceptor(new String[]{"/**"}, new CacheControlInterceptor());
	}
	
	@Bean
	public SampleResourceProcessor getMongoSampleResourceProcessor() {
		return new SampleResourceProcessor();
	}
	
	@Bean
	public HttpMessageConverter<MongoSample> getXmlSampleHttpMessageConverter() {
		return new XmlSampleHttpMessageConverter();
	}
	
}
