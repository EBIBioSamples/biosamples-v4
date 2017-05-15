package uk.ac.ebi.biosamples.client;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class BioSamplesClientConfig {
	
	//sets resttemplate to use connection pooling
	@Bean
	public ClientHttpRequestFactory getClientHttpRequestFactory() {
    	//TODO add application properties to configure this

    	PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    	connectionManager.setMaxTotal(64);
    	connectionManager.setDefaultMaxPerRoute(64);
    	connectionManager.setValidateAfterInactivity(1000);
    	
    	ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy() {
    		private Logger log = LoggerFactory.getLogger(getClass());
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
        CacheConfig cacheConfig = 
        		CacheConfig.custom()
                .setMaxCacheEntries(1000) //cache up to 1000 responses
                .setMaxObjectSize(8*1024*1024) //cache up to 8Mb per response
                .setSharedCache(false) //behave like a browser cache
                .build();
                
        //enforce a timeout of 60 seconds
        RequestConfig requestConfig = RequestConfig.custom()
        		  .setConnectTimeout(60 * 1000)
        		  .setConnectionRequestTimeout(60 * 1000)
        		  .setSocketTimeout(60 * 1000)
        		  .build();
        
        //build the final client
    	CloseableHttpClient httpClient = 
    			CachingHttpClients.custom()
    	        .setCacheConfig(cacheConfig)
    			.setKeepAliveStrategy(keepAliveStrategy)
    			.setConnectionManager(connectionManager)
    			.setDefaultRequestConfig(requestConfig)
    			.build();

    	return new HttpComponentsClientHttpRequestFactory(httpClient);
	}	
	
	
	@Bean
	public RestTemplateCustomizer customizeClientHttopRequestFactory(ClientHttpRequestFactory clientHttpRequestFactory) {
		return new RestTemplateCustomizer() {
			@Override
			public void customize(RestTemplate restTemplate) {
				restTemplate.setRequestFactory(clientHttpRequestFactory);
			}			
		};
	}
	
	
	
	/**
	 * this is not idea, because it will apply to all resttempalte and will
	 * limit them to ONLY string or hal or json
	 * TODO do this better!
	 * @return
	 */
	@Bean
	public RestTemplateCustomizer customizeHttpMessageConverters() {
		return new RestTemplateCustomizer() {
			@Override
			public void customize(RestTemplate restTemplate) {
				List<HttpMessageConverter<?>> converters = new ArrayList<>();
				
				converters.add(new StringHttpMessageConverter(Charset.forName("UTF-8")));

				ObjectMapper mapper = new ObjectMapper();
				mapper.registerModule(new Jackson2HalModule());
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

				converter.setObjectMapper(mapper);
				converter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON, MediaType.APPLICATION_JSON));

				converters.add(converter);
				
				restTemplate.setMessageConverters(converters);
			}			
		};
	}
	

	
}
