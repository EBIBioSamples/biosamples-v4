package uk.ac.ebi.biosamples;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Application {
	
	//this is needed to read nonstrings from properties files
	//must be static for lifecycle reasons
	@Bean
	public static PropertySourcesPlaceholderConfigurer getPropertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
	//sets resttemplate to use connection pooling
	@Bean
	public ClientHttpRequestFactory getClientHttpRequestFactory() {

    	PoolingHttpClientConnectionManager conman = new PoolingHttpClientConnectionManager();
    	conman.setMaxTotal(128);
    	conman.setDefaultMaxPerRoute(64);
    	conman.setValidateAfterInactivity(0);
    	
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
    	
    	CloseableHttpClient httpClient = HttpClients.custom()
    			.setKeepAliveStrategy(keepAliveStrategy)
    			.setConnectionManager(conman).build();
    	
    	return new HttpComponentsClientHttpRequestFactory(httpClient);
	}
	
	@Bean
	public RestTemplate getRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(getClientHttpRequestFactory());
		return restTemplate;
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
