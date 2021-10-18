/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
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
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Application {

  @Bean
  public RestTemplate restTemplate(RestTemplateCustomizer restTemplateCustomizer) {
    RestTemplate restTemplate = new RestTemplate();
    restTemplateCustomizer.customize(restTemplate);
    return restTemplate;
  }

  public static void main(String[] args) {
    System.exit(SpringApplication.exit(SpringApplication.run(Application.class, args)));
  }

  @Bean
  public RestTemplateCustomizer getRestTemplateCustomizer() {
    return new RestTemplateCustomizer() {

      public void customize(RestTemplate restTemplate) {

        // use a keep alive strategy to try to make it easier to maintain connections for
        // reuse
        ConnectionKeepAliveStrategy keepAliveStrategy =
            new ConnectionKeepAliveStrategy() {
              public long getKeepAliveDuration(HttpResponse response, HttpContext context) {

                // check if there is a non-standard keep alive header present
                HeaderElementIterator it =
                    new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                  HeaderElement he = it.nextElement();
                  String param = he.getName();
                  String value = he.getValue();
                  if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                  }
                }
                // default to 15s if no header
                return 15 * 1000;
              }
            };

        // set a number of connections to use at once for multiple threads
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
            new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(8);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(8);

        // set a local cache for cacheable responses
        // TODO application.properties this
        CacheConfig cacheConfig =
            CacheConfig.custom()
                .setMaxCacheEntries(1024)
                .setMaxObjectSize(1024 * 1024) // max size of 1Mb
                // number of entries x size of entries = 1Gb total cache size
                .setSharedCache(false) // act like a browser cache not a middle-hop cache
                .build();

        // set a timeout limit
        int timeout = 60000;
        RequestConfig config =
            RequestConfig.custom()
                .setConnectTimeout(timeout) // time to establish the connection with the remote
                // host
                .setConnectionRequestTimeout(timeout) // maximum time of inactivity between two data
                // packets
                .setSocketTimeout(timeout)
                .build(); // time to wait for a connection from the connection
        // manager/pool

        // set retry strategy to retry on any 5xx error
        // ebi load balancers return a 500 error when a service is unavaliable not a 503
        ServiceUnavailableRetryStrategy serviceUnavailStrategy =
            new ServiceUnavailableRetryStrategy() {
              private final int maxRetries = 100;
              private final int retryInterval = 1000;

              public boolean retryRequest(
                  HttpResponse response, int executionCount, HttpContext context) {
                return executionCount <= maxRetries
                    && (response.getStatusLine().getStatusCode()
                            == HttpStatus.SC_SERVICE_UNAVAILABLE
                        || response.getStatusLine().getStatusCode()
                            == HttpStatus.SC_INTERNAL_SERVER_ERROR);
              }

              public long getRetryInterval() {
                // measured in milliseconds
                return retryInterval;
              }
            };

        // make the actual client
        HttpClient httpClient =
            CachingHttpClientBuilder.create()
                .setCacheConfig(cacheConfig)
                .useSystemProperties()
                .setConnectionManager(poolingHttpClientConnectionManager)
                .setKeepAliveStrategy(keepAliveStrategy)
                .setServiceUnavailableRetryStrategy(serviceUnavailStrategy)
                .setDefaultRequestConfig(config)
                .build();

        // and wire it into the resttemplate
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

        // make sure there is a application/hal+json converter
        // traverson will make its own but not if we want to customize the resttemplate in
        // any way
        // (e.g. caching)
        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jackson2HalModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MappingJackson2HttpMessageConverter halConverter =
            new TypeConstrainedMappingJackson2HttpMessageConverter(RepresentationModel.class);
        halConverter.setObjectMapper(mapper);
        halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
        // make sure this is inserted first
        converters.add(0, halConverter);
        restTemplate.setMessageConverters(converters);
      }
    };
  }
}
