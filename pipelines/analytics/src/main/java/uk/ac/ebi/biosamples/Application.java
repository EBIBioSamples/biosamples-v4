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

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoute;
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
import uk.ac.ebi.biosamples.mongo.MongoProperties;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;

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
  public RestTemplateCustomizer restTemplateCustomizer(
      BioSamplesProperties bioSamplesProperties, PipelinesProperties piplinesProperties) {
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
                // default to 60s if no header
                return 60 * 1000;
              }
            };

        // set a number of connections to use at once for multiple threads
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
            new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(piplinesProperties.getConnectionCountMax());
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(
            piplinesProperties.getConnectionCountDefault());
        poolingHttpClientConnectionManager.setMaxPerRoute(
            new HttpRoute(HttpHost.create(piplinesProperties.getZooma())),
            piplinesProperties.getConnectionCountZooma());
        poolingHttpClientConnectionManager.setMaxPerRoute(
            new HttpRoute(HttpHost.create(bioSamplesProperties.getOls())),
            piplinesProperties.getConnectionCountOls());

        // set a local cache for cacheable responses
        CacheConfig cacheConfig =
            CacheConfig.custom()
                .setMaxCacheEntries(1024)
                .setMaxObjectSize(1024 * 1024) // max size of 1Mb
                // number of entries x size of entries = 1Gb total cache size
                .setSharedCache(false) // act like a browser cache not a middle-hop cache
                .build();

        // set a timeout limit
        // TODO put this in application.properties
        int timeout = 60; // in seconds
        RequestConfig config =
            RequestConfig.custom()
                .setConnectTimeout(timeout * 1000) // time to establish the connection with the
                // remote host
                .setConnectionRequestTimeout(
                    timeout * 1000) // maximum time of inactivity between two
                // data packets
                .setSocketTimeout(timeout * 1000)
                .build(); // time to wait for a connection from the connection
        // manager/pool

        // make the actual client
        HttpClient httpClient =
            CachingHttpClientBuilder.create()
                .setCacheConfig(cacheConfig)
                .useSystemProperties()
                .setConnectionManager(poolingHttpClientConnectionManager)
                .setKeepAliveStrategy(keepAliveStrategy)
                .setDefaultRequestConfig(config)
                .build();

        // and wire it into the resttemplate
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
      }
    };
  }

  @Bean(name = "SampleAccessionService")
  public MongoAccessionService mongoSampleAccessionService(
      MongoSampleRepository mongoSampleRepository,
      SampleToMongoSampleConverter sampleToMongoSampleConverter,
      MongoSampleToSampleConverter mongoSampleToSampleConverter,
      MongoProperties mongoProperties) {
    return new MongoAccessionService(
        mongoSampleRepository,
        sampleToMongoSampleConverter,
        mongoSampleToSampleConverter,
        mongoProperties.getAccessionPrefix(),
        mongoProperties.getAccessionMinimum(),
        mongoProperties.getAcessionQueueSize());
  }
}
