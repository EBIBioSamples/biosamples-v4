package uk.ac.ebi.biosamples;

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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableCaching
public class Application {

    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(Application.class, args));
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateCustomizer restTemplateCustomizer) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplateCustomizer.customize(restTemplate);
        return restTemplate;
    }

    @Bean
    public RestTemplateCustomizer restTemplateCustomizer(BioSamplesProperties bioSamplesProperties, PipelinesProperties piplinesProperties) {
        return new RestTemplateCustomizer() {
            public void customize(RestTemplate restTemplate) {

                //use a keep alive strategy to try to make it easier to maintain connections for reuse
                ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy() {
                    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {

                        //check if there is a non-standard keep alive header present
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
                        //default to 60s if no header
                        return 60 * 1000;
                    }
                };

                //set a number of connections to use at once for multiple threads
                PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
                poolingHttpClientConnectionManager.setMaxTotal(piplinesProperties.getConnectionCountMax());
                poolingHttpClientConnectionManager.setDefaultMaxPerRoute(piplinesProperties.getConnectionCountDefault());

                //set a local cache for cacheable responses
                CacheConfig cacheConfig = CacheConfig.custom()
                        .setMaxCacheEntries(1024)
                        .setMaxObjectSize(1024 * 1024) //max size of 1Mb
                        //number of entries x size of entries = 1Gb total cache size
                        .setSharedCache(false) //act like a browser cache not a middle-hop cache
                        .build();

                //set a timeout limit
                //TODO put this in application.properties
                int timeout = 60; //in seconds
                RequestConfig config = RequestConfig.custom()
                        .setConnectTimeout(timeout * 1000) //time to establish the connection with the remote host
                        .setConnectionRequestTimeout(timeout * 1000) //maximum time of inactivity between two data packets
                        .setSocketTimeout(timeout * 1000).build(); //time to wait for a connection from the connection manager/pool


                //make the actual client
                HttpClient httpClient = CachingHttpClientBuilder.create()
                        .setCacheConfig(cacheConfig)
                        .useSystemProperties()
                        .setConnectionManager(poolingHttpClientConnectionManager)
                        .setKeepAliveStrategy(keepAliveStrategy)
                        .setDefaultRequestConfig(config)
                        .build();

                //and wire it into the resttemplate
                restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
            }
        };
    }
}
