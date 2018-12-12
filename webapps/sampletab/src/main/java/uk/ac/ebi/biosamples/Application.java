package uk.ac.ebi.biosamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.MongoProperties;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.service.XmlAsSampleHttpMessageConverter;
import uk.ac.ebi.biosamples.service.XmlGroupToSampleConverter;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;

//import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@Configuration
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public HttpMessageConverter<Sample> getXmlSampleHttpMessageConverter(XmlSampleToSampleConverter xmlSampleToSampleConverter,
			XmlGroupToSampleConverter xmlGroupToSampleConverter) {
		return new XmlAsSampleHttpMessageConverter(xmlSampleToSampleConverter, xmlGroupToSampleConverter);
	}

    @Bean
    public MongoAccessionService mongoGroupAccessionService(MongoSampleRepository mongoSampleRepository, SampleToMongoSampleConverter sampleToMongoSampleConverter,
			MongoSampleToSampleConverter mongoSampleToSampleConverter, MongoProperties mongoProperties) {
    	return new MongoAccessionService(mongoSampleRepository, sampleToMongoSampleConverter,
    			mongoSampleToSampleConverter, "SAMEG", 
    			mongoProperties.getAccessionMinimum(), mongoProperties.getAcessionQueueSize());
    }

}
