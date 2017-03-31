package uk.ac.ebi.biosamples.client;

import java.util.List;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class BioSamplesClientConfig {

	
	//sets resttemplate to use connection pooling
	@Bean
	@ConditionalOnMissingBean
	public ClientHttpRequestFactory getClientHttpRequestFactory() {

    	PoolingHttpClientConnectionManager conman = new PoolingHttpClientConnectionManager();
    	conman.setMaxTotal(64);
    	conman.setDefaultMaxPerRoute(64);
    	conman.setValidateAfterInactivity(1000);
    	
    	ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            	//see if the user provides a live time
                HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        return Long.parseLong(value) * 1000;
                    }
                }
                //default to one second live time 
                return 1 * 1000;
            }
        };
    	
        //use a caching http client to respect cache-content header
        CacheConfig cacheConfig = CacheConfig.custom()
                .setMaxCacheEntries(1000)
                .setMaxObjectSize(8192)
                .build();
        
    	CloseableHttpClient httpClient = 
    			CachingHttpClients.custom()
    	        .setCacheConfig(cacheConfig)
    			.setKeepAliveStrategy(keepAliveStrategy)
    			.setConnectionManager(conman).build();

    	//TODO add application properties to configure this
    	return new HttpComponentsClientHttpRequestFactory(httpClient);
	}
	
	/**
	 * Creates a rest template for use with the other components. Configures it to support hal+json
	 * and to use connection pooling
	 *  
	 * @param clientHttpRequestFactory
	 * @param mapper
	 * @return
	 */
	@Bean
	@ConditionalOnMissingBean
	public RestOperations getRestOperations(ClientHttpRequestFactory clientHttpRequestFactory, ObjectMapper mapper) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(clientHttpRequestFactory);
		
		//need to create a new message converter to handle hal+json
		//ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new Jackson2HalModule());
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setSupportedMediaTypes(MediaType.parseMediaTypes("application/hal+json"));
		converter.setObjectMapper(mapper);

		//add the new converters to the restTemplate
		//but make sure it is BEFORE the existing converters
		List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
		converters.add(0,converter);
		restTemplate.setMessageConverters(converters);
		
		
		return restTemplate;
	}
}
