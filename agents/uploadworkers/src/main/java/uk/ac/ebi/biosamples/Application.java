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
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.mongo.repository.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;

@SpringBootApplication
public class Application {
  public static void main(final String[] args) {
    System.exit(SpringApplication.exit(SpringApplication.run(Application.class, args)));
  }

  @Bean("biosamplesFileUploadSubmissionContainerFactory")
  public SimpleRabbitListenerContainerFactory containerFactory(
      final SimpleRabbitListenerContainerFactoryConfigurer configurer,
      final ConnectionFactory connectionFactory) {
    final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConcurrentConsumers(5);
    factory.setMaxConcurrentConsumers(5);
    configurer.configure(factory, connectionFactory);

    return factory;
  }

  @Bean(name = "SampleAccessionService")
  public MongoAccessionService mongoSampleAccessionService(
      final MongoSampleRepository mongoSampleRepository,
      final SampleToMongoSampleConverter sampleToMongoSampleConverter,
      final MongoSampleToSampleConverter mongoSampleToSampleConverter,
      final MongoOperations mongoOperations) {
    return new MongoAccessionService(
        mongoSampleRepository,
        sampleToMongoSampleConverter,
        mongoSampleToSampleConverter,
        mongoOperations);
  }


  // todo I Had to add restTemplate bean as a temporary workaround as there seems to be a problem with dependencies after refactor.
  //  We need to sort out dependency problem and remove this unused dependency.

  @Bean
  public RestTemplate restTemplate(final RestTemplateCustomizer restTemplateCustomizer) {
    final RestTemplate restTemplate = new RestTemplate();
    restTemplateCustomizer.customize(restTemplate);
    return restTemplate;
  }

  @Bean
  public RestTemplateCustomizer restTemplateCustomizer(final BioSamplesProperties bioSamplesProperties) {
    return restTemplate -> {

      final ConnectionKeepAliveStrategy keepAliveStrategy =
          (response, context) -> {

            final HeaderElementIterator it =
                new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
              final HeaderElement he = it.nextElement();
              final String param = he.getName();
              final String value = he.getValue();
              if (value != null && param.equalsIgnoreCase("timeout")) {
                return Long.parseLong(value) * 1000;
              }
            }

            return 60 * 1000;
          };

      final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
          new PoolingHttpClientConnectionManager();
      poolingHttpClientConnectionManager.setMaxTotal(8);
      poolingHttpClientConnectionManager.setDefaultMaxPerRoute(8);

      final CacheConfig cacheConfig =
          CacheConfig.custom()
              .setMaxCacheEntries(1024)
              .setMaxObjectSize(1024 * 1024)
              .setSharedCache(false)
              .build();
      final int timeout = 60;
      final RequestConfig config =
          RequestConfig.custom()
              .setConnectTimeout(timeout * 1000)
              .setConnectionRequestTimeout(
                  timeout * 1000)
              .setSocketTimeout(timeout * 1000)
              .build();
      final HttpClient httpClient =
          CachingHttpClientBuilder.create()
              .setCacheConfig(cacheConfig)
              .useSystemProperties()
              .setConnectionManager(poolingHttpClientConnectionManager)
              .setKeepAliveStrategy(keepAliveStrategy)
              .setDefaultRequestConfig(config)
              .build();
      restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    };
  }
}
