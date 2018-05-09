package uk.ac.ebi.biosamples;

import java.util.concurrent.Executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.UrlTemplateResolver;

import com.github.benmanes.caffeine.cache.CaffeineSpec;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.MongoProperties;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.service.SampleAsXMLHttpMessageConverter;
import uk.ac.ebi.biosamples.service.SampleToXmlConverter;

//import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@EnableAsync
@EnableCaching
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public HttpMessageConverter<Sample> getXmlSampleHttpMessageConverter(SampleToXmlConverter sampleToXmlConverter) {
		return new SampleAsXMLHttpMessageConverter(sampleToXmlConverter);
	}

    @Bean(name = "threadPoolTaskExecutor")
    public Executor threadPoolTaskExecutor() {
    	ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
    	ex.setMaxPoolSize(128);
    	ex.setQueueCapacity(2056);
    	return ex;
    }
    
    @Bean
    public RepositoryDetectionStrategy repositoryDetectionStrategy() {
    	return RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED;
    }


    /* Necessary to render XML using Jaxb2 annotations */
	@Bean
	public Jaxb2RootElementHttpMessageConverter jaxb2RootElementHttpMessageConverter() {
		return new Jaxb2RootElementHttpMessageConverter();
	}

    @Bean
    public CaffeineSpec CaffeineSpec() {
    	return CaffeineSpec.parse("maximumSize=500,expireAfterWrite=60s");
    }

    @Bean
    public ITemplateResolver templateResolver() {
    	return new UrlTemplateResolver();
    }

    @Bean
    public MongoAccessionService mongoSampleAccessionService(MongoSampleRepository mongoSampleRepository, SampleToMongoSampleConverter sampleToMongoSampleConverter,
			MongoSampleToSampleConverter mongoSampleToSampleConverter, MongoProperties mongoProperties) {
    	return new MongoAccessionService(mongoSampleRepository, sampleToMongoSampleConverter,
    			mongoSampleToSampleConverter, mongoProperties.getAccessionPrefix(), 
    			mongoProperties.getAccessionMinimum(), mongoProperties.getAcessionQueueSize());
    }

}
