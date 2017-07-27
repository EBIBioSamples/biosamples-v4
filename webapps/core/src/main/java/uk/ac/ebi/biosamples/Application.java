package uk.ac.ebi.biosamples;

import java.util.concurrent.Executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.handler.MappedInterceptor;

import com.github.benmanes.caffeine.cache.CaffeineSpec;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.NeoProperties;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.service.NeoAccessionService;
import uk.ac.ebi.biosamples.service.CacheControlInterceptor;
import uk.ac.ebi.biosamples.xml.XmlSampleHttpMessageConverter;

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
	public HttpMessageConverter<Sample> getXmlSampleHttpMessageConverter() {
		return new XmlSampleHttpMessageConverter();
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
    
    @Bean
    public NeoAccessionService neoAccessionService(NeoSampleRepository neoSampleRepository, NeoProperties neoProperties) {
    	return new NeoAccessionService(neoSampleRepository, neoProperties);
    }
    
    
    @Bean
    public CaffeineSpec CaffeineSpec() {
    	return CaffeineSpec.parse("maximumSize=500,expireAfterWrite=60s");
    }
    
    /*
    @Bean
    public ApplicationRunner appRunner() {
    	return new ApplicationRunner() {

    		@Autowired
    		private NeoSampleRepository neoSampleRepository;
    		
			@Override
			public void run(ApplicationArguments args) throws Exception {
				NeoSample sample = NeoSample.build("TestInsert", "foo", LocalDateTime.now(), LocalDateTime.now(), null, null, null);
				neoSampleRepository.insertNew(sample);
				//this should throw an exception?
				neoSampleRepository.insertNew(sample);
			}    		
    	};
    }
    */
}
