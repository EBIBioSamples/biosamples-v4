package uk.ac.ebi.biosamples;

import com.github.benmanes.caffeine.cache.CaffeineSpec;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.UrlTemplateResolver;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.CacheControlInterceptor;
import uk.ac.ebi.biosamples.service.SampleToXmlConverter;
import uk.ac.ebi.biosamples.service.XmlSampleHttpMessageConverter;
import uk.ac.ebi.biosamples.service.XmlToSampleConverter;

import java.util.concurrent.Executor;

//import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

//@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@ComponentScan(lazyInit = true)
@EnableAsync
@EnableCaching
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public MappedInterceptor getCacheHeaderMappedInterceptor() {
	    return new MappedInterceptor(new String[]{"/**"}, new CacheControlInterceptor());
	}
	
	@Bean
	public HttpMessageConverter<Sample> getXmlSampleHttpMessageConverter(SampleToXmlConverter sampleToXmlConverter, XmlToSampleConverter xmlToSampleConverter) {
		return new XmlSampleHttpMessageConverter(sampleToXmlConverter, xmlToSampleConverter);
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
    

}
