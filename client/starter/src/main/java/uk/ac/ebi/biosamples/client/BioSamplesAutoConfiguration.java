/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
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
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.model.auth.AuthRealm;
import uk.ac.ebi.biosamples.client.service.ClientService;
import uk.ac.ebi.biosamples.client.service.WebinAuthClientService;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.core.service.AttributeValidator;
import uk.ac.ebi.biosamples.core.service.SampleValidator;

@Configuration
@AutoConfigureAfter(WebClientAutoConfiguration.class)
public class BioSamplesAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(AttributeValidator.class)
  public AttributeValidator attributeValidator() {
    return new AttributeValidator();
  }

  @Bean
  @ConditionalOnMissingBean(SampleValidator.class)
  public SampleValidator sampleValidator(final AttributeValidator attributeValidator) {
    return new SampleValidator(attributeValidator);
  }

  @Bean
  @ConditionalOnMissingBean(ClientProperties.class)
  public ClientProperties clientProperties() {
    return new ClientProperties();
  }

  @Bean
  @Primary
  @ConditionalOnMissingBean(WebinAuthClientService.class)
  public WebinAuthClientService webinAuthClientService(
      final RestTemplateBuilder restTemplateBuilder, final ClientProperties clientProperties) {
    if (clientProperties.getBiosamplesClientWebinUsername() != null
        && clientProperties.getBiosamplesClientWebinPassword() != null) {
      return new WebinAuthClientService(
          restTemplateBuilder,
          clientProperties.getBiosamplesWebinAuthTokenUri(),
          clientProperties.getBiosamplesClientWebinUsername(),
          clientProperties.getBiosamplesClientWebinPassword(),
          Arrays.asList(AuthRealm.ENA, AuthRealm.EGA)); // pass the realm
    } else {
      return null;
    }
  }

  @Bean
  public WebinAuthClientService webinAuthTestClientService(
      final RestTemplateBuilder restTemplateBuilder, final ClientProperties clientProperties) {
    if (clientProperties.getBiosamplesClientWebinUsername() != null
        && clientProperties.getBiosamplesClientWebinPassword() != null) {
      return new WebinAuthClientService(
          restTemplateBuilder,
          clientProperties.getBiosamplesWebinAuthTokenUri(),
          clientProperties.getBiosamplesClientWebinTestUsername(),
          clientProperties.getBiosamplesClientWebinPassword(),
          Arrays.asList(AuthRealm.ENA, AuthRealm.EGA)); // pass the realm
    } else {
      return null;
    }
  }

  @Bean("WEBINCLIENT")
  @Primary
  public BioSamplesClient bioSamplesWebinClient(
      final ClientProperties clientProperties,
      RestTemplateBuilder restTemplateBuilder,
      final SampleValidator sampleValidator) {
    restTemplateBuilder =
        restTemplateBuilder.additionalCustomizers(
            new BioSampleClientRestTemplateCustomizer(clientProperties));
    final ClientService clientService =
        webinAuthClientService(restTemplateBuilder, clientProperties);

    return new BioSamplesClient(
        clientProperties.getBiosamplesClientUri(),
        clientProperties.getBiosamplesClientUriV2(),
        restTemplateBuilder,
        sampleValidator,
        clientService,
        clientProperties);
  }

  @Bean("WEBINTESTCLIENT")
  public BioSamplesClient bioSamplesWebinTestClient(
      final ClientProperties clientProperties,
      RestTemplateBuilder restTemplateBuilder,
      final SampleValidator sampleValidator) {
    restTemplateBuilder =
        restTemplateBuilder.additionalCustomizers(
            new BioSampleClientRestTemplateCustomizer(clientProperties));
    final ClientService clientService =
        webinAuthTestClientService(restTemplateBuilder, clientProperties);

    return new BioSamplesClient(
        clientProperties.getBiosamplesClientUri(),
        clientProperties.getBiosamplesClientUriV2(),
        restTemplateBuilder,
        sampleValidator,
        clientService,
        clientProperties);
  }

  private static class BioSampleClientRestTemplateCustomizer implements RestTemplateCustomizer {

    private final ClientProperties clientProperties;

    BioSampleClientRestTemplateCustomizer(final ClientProperties clientProperties) {
      this.clientProperties = clientProperties;
    }

    @Override
    public void customize(final RestTemplate restTemplate) {
      // use a keep alive strategy to try to make it easier to maintain connections for reuse
      final ConnectionKeepAliveStrategy keepAliveStrategy =
          new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(
                final HttpResponse response, final HttpContext context) {

              // check if there is a non-standard keep alive header present
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
              // default to 15s if no header
              return 15 * 1000;
            }
          };

      // set a number of connections to use at once for multiple threads
      final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
          new PoolingHttpClientConnectionManager();
      poolingHttpClientConnectionManager.setMaxTotal(
          clientProperties.getBiosamplesClientConnectionCountMax());
      poolingHttpClientConnectionManager.setDefaultMaxPerRoute(
          clientProperties.getBiosamplesClientConnectionCountDefault());

      // set a local cache for cacheable responses
      final CacheConfig cacheConfig =
          CacheConfig.custom()
              .setMaxCacheEntries(clientProperties.getBiosamplesClientCacheMaxEntries())
              .setMaxObjectSize(
                  clientProperties.getBiosamplesClientCacheMaxObjectSize()) // max size of
              // 1Mb
              // number of entries x size of entries = 1Gb total cache size
              .setSharedCache(false) // act like a browser cache not a middle-hop cache
              .build();

      // set a timeout limit
      final int timeout = clientProperties.getBiosamplesClientTimeout();
      final RequestConfig config =
          RequestConfig.custom()
              .setConnectTimeout(timeout) // time to establish the connection with the remote
              // host
              .setConnectionRequestTimeout(
                  timeout) // maximum time of inactivity between two data packets
              .setSocketTimeout(timeout)
              .build(); // time to wait for a connection from the connection
      // manager/pool

      // set retry strategy to retry on any 5xx error
      // ebi load balancers return a 500 error when a service is unavaliable not a 503
      final ServiceUnavailableRetryStrategy serviceUnavailStrategy =
          new ServiceUnavailableRetryStrategy() {

            @Override
            public boolean retryRequest(
                final HttpResponse response, final int executionCount, final HttpContext context) {
              final int maxRetries = 100;
              return executionCount <= maxRetries
                  && (response.getStatusLine().getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE
                      || response.getStatusLine().getStatusCode()
                          == HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }

            @Override
            public long getRetryInterval() {
              // measured in milliseconds
              return 1000;
            }
          };

      // make the actual client
      final HttpClient httpClient =
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
      // traverson will make its own but not if we want to customize the resttemplate in any
      // way
      // (e.g. caching)
      final List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
      final ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new Jackson2HalModule());
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      final MappingJackson2HttpMessageConverter halConverter =
          new TypeConstrainedMappingJackson2HttpMessageConverter(RepresentationModel.class);
      halConverter.setObjectMapper(mapper);
      halConverter.setSupportedMediaTypes(Collections.singletonList(MediaTypes.HAL_JSON));
      // make sure this is inserted first
      converters.add(0, halConverter);
      restTemplate.setMessageConverters(converters);
    }
  }
}
