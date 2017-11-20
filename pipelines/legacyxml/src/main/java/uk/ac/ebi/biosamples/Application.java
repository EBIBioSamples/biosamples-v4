package uk.ac.ebi.biosamples;

import java.util.Arrays;
import java.util.List;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@EnableCaching(proxyTargetClass = true)
@EnableAsync
@EnableScheduling
public class Application {
	
	//this is needed to read nonstrings from properties files
	//must be static for lifecycle reasons
	@Bean
	public PropertySourcesPlaceholderConfigurer getPropertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
	public static void main(String[] args) {
		SpringApplication.exit(SpringApplication.run(Application.class, args));
	}
}
