package uk.ac.ebi.biosamples;

import java.util.List;
import java.util.concurrent.Executor;

import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.handler.MappedInterceptor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientOptions;
import com.mongodb.ReadPreference;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.xml.XmlSampleHttpMessageConverter;

@SpringBootApplication
//@EnableHypermediaSupport(type = { EnableHypermediaSupport.HypermediaType.HAL })
@EnableAsync
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
    	ex.setMaxPoolSize(16);
    	ex.setQueueCapacity(128);
    	return ex;
    }

    @Value("${spring.data.solr.host}")
    String solrHost;

    //workaround for 1.5.x problems with multicore solr
    @Bean
    @Primary
    public SolrTemplate solrTemplate(){
        CustomSolrTemplate template = new CustomSolrTemplate(new HttpSolrClient(solrHost));
        return template;
    }
    
}
