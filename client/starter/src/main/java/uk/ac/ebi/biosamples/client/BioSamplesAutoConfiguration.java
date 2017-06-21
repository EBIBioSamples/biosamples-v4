package uk.ac.ebi.biosamples.client;

import java.util.Arrays;
import java.util.List;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.service.AttributeValidator;
import uk.ac.ebi.biosamples.service.SampleValidator;

@Configuration
@ConditionalOnMissingBean(BioSamplesClient.class)
public class BioSamplesAutoConfiguration {

	@Bean	
	@ConditionalOnMissingBean(AttributeValidator.class)
	public AttributeValidator attributeValidator() {
		return new AttributeValidator();
	}
	
	@Bean	
	@ConditionalOnMissingBean(SampleValidator.class)
	public SampleValidator sampleValidator(AttributeValidator attributeValidator) {
		return new SampleValidator(attributeValidator);
	}
	
	@Bean	
	@ConditionalOnMissingBean(ClientProperties.class)
	public ClientProperties clientProperties() {
		return new ClientProperties();
	}
	
	@Bean	
	@ConditionalOnMissingBean(AapClientService.class)
	public AapClientService aapClientService(RestTemplateBuilder restTemplateBuilder, ClientProperties clientProperties) {
		return new AapClientService(restTemplateBuilder, clientProperties);
	}
	
	@Bean
	public BioSamplesClient bioSamplesClient(ClientProperties clientProperties, 
			RestTemplateBuilder restTemplateBuilder, SampleValidator sampleValidator, AapClientService aapClientService) {		
		restTemplateBuilder = restTemplateBuilder.additionalCustomizers(new RestTemplateCustomizer() {
			public void customize(RestTemplate restTemplate) {
				
				//use a keep alive strategy to try to make it easier to maintain connections for reuse
				ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy() {
				    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
				        HeaderElementIterator it = new BasicHeaderElementIterator
				            (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
				        while (it.hasNext()) {
				            HeaderElement he = it.nextElement();
				            String param = he.getName();
				            String value = he.getValue();
				            if (value != null && param.equalsIgnoreCase
				               ("timeout")) {
				                return Long.parseLong(value) * 1000;
				            }
				        }
				        //default to 15s
				        return 15 * 1000;
				    }
				};
				
				//set a number of connections to use at once for multiple threads
				//TODO put this in application.properties
				PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
				poolingHttpClientConnectionManager.setDefaultMaxPerRoute(16);
				poolingHttpClientConnectionManager.setMaxTotal(16);
				
				
				//set a timeout limit
				//TODO put this in application.properties
				int timeout = 30; //in seconds
				RequestConfig config = RequestConfig.custom()
				  .setConnectTimeout(timeout * 1000) //time to establish the connection with the remote host
				  .setConnectionRequestTimeout(timeout * 1000) //maximum time of inactivity between two data packets
				  .setSocketTimeout(timeout * 1000).build(); //time to wait for a connection from the connection manager/pool
				
				
				//make the actual client
				HttpClient httpClient = HttpClientBuilder.create()
						.setConnectionManager(poolingHttpClientConnectionManager)
						.setKeepAliveStrategy(keepAliveStrategy)
						.setDefaultRequestConfig(config)
						.build();
				
				//and wire it into the resttemplate
		        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

				//make sure there is a application/hal+json converter		
				//traverson will make its own but not if we want to customize the resttemplate in any way (e.g. caching)
				List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();				
				ObjectMapper mapper = new ObjectMapper();
				mapper.registerModule(new Jackson2HalModule());
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
				halConverter.setObjectMapper(mapper);
				halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
				//make sure this is inserted first
				converters.add(0, halConverter);				
				restTemplate.setMessageConverters(converters);
			}			
		});
		return new BioSamplesClient(clientProperties, restTemplateBuilder, sampleValidator, aapClientService);
	}
}
