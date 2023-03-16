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
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.model.auth.AuthRealm;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.client.service.ClientService;
import uk.ac.ebi.biosamples.client.service.WebinAuthClientService;
import uk.ac.ebi.biosamples.service.AttributeValidator;
import uk.ac.ebi.biosamples.service.SampleValidator;

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
  @ConditionalOnMissingBean(BioSamplesProperties.class)
  public BioSamplesProperties bioSamplesProperties() {
    return new BioSamplesProperties();
  }

  @Bean("AAP")
  @ConditionalOnMissingBean(AapClientService.class)
  public AapClientService aapClientService(
      final RestTemplateBuilder restTemplateBuilder,
      final BioSamplesProperties bioSamplesProperties) {
    if (bioSamplesProperties.getBiosamplesClientAapUsername() != null
        && bioSamplesProperties.getBiosamplesClientAapPassword() != null) {
      return new AapClientService(
          restTemplateBuilder,
          bioSamplesProperties.getBiosamplesClientAapUri(),
          bioSamplesProperties.getBiosamplesClientAapUsername(),
          bioSamplesProperties.getBiosamplesClientAapPassword());
    } else {
      return null;
    }
  }

  @Bean("WEBIN")
  @ConditionalOnMissingBean(WebinAuthClientService.class)
  public WebinAuthClientService webinAuthClientService(
      final RestTemplateBuilder restTemplateBuilder,
      final BioSamplesProperties bioSamplesProperties) {
    if (bioSamplesProperties.getBiosamplesClientWebinUsername() != null
        && bioSamplesProperties.getBiosamplesClientWebinPassword() != null) {
      return new WebinAuthClientService(
          restTemplateBuilder,
          bioSamplesProperties.getBiosamplesWebinAuthTokenUri(),
          bioSamplesProperties.getBiosamplesClientWebinUsername(),
          bioSamplesProperties.getBiosamplesClientWebinPassword(),
          Arrays.asList(AuthRealm.ENA, AuthRealm.EGA)); // pass the realm
    } else {
      return null;
    }
  }

  @Bean("WEBINCLIENT")
  public BioSamplesClient bioSamplesWebinClient(
      final BioSamplesProperties bioSamplesProperties,
      RestTemplateBuilder restTemplateBuilder,
      final SampleValidator sampleValidator) {
    restTemplateBuilder =
        restTemplateBuilder.additionalCustomizers(
            new BioSampleClientRestTemplateCustomizer(bioSamplesProperties));
    final ClientService clientService =
        webinAuthClientService(restTemplateBuilder, bioSamplesProperties);

    return new BioSamplesClient(
        bioSamplesProperties.getBiosamplesClientUri(),
        bioSamplesProperties.getBiosamplesClientUriV2(),
        restTemplateBuilder,
        sampleValidator,
        clientService,
        bioSamplesProperties);
  }

  @Bean("AAPCLIENT")
  @Primary
  public BioSamplesClient bioSamplesAapClient(
      final BioSamplesProperties bioSamplesProperties,
      RestTemplateBuilder restTemplateBuilder,
      final SampleValidator sampleValidator) {
    restTemplateBuilder =
        restTemplateBuilder.additionalCustomizers(
            new BioSampleClientRestTemplateCustomizer(bioSamplesProperties));
    final ClientService clientService = aapClientService(restTemplateBuilder, bioSamplesProperties);

    return new BioSamplesClient(
        bioSamplesProperties.getBiosamplesClientUri(),
        bioSamplesProperties.getBiosamplesClientUriV2(),
        restTemplateBuilder,
        sampleValidator,
        clientService,
        bioSamplesProperties);
  }

  private static class BioSampleClientRestTemplateCustomizer implements RestTemplateCustomizer {

    private final BioSamplesProperties bioSamplesProperties;

    BioSampleClientRestTemplateCustomizer(final BioSamplesProperties bioSamplesProperties) {
      this.bioSamplesProperties = bioSamplesProperties;
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
          bioSamplesProperties.getBiosamplesClientConnectionCountMax());
      poolingHttpClientConnectionManager.setDefaultMaxPerRoute(
          bioSamplesProperties.getBiosamplesClientConnectionCountDefault());

      // set a local cache for cacheable responses
      final CacheConfig cacheConfig =
          CacheConfig.custom()
              .setMaxCacheEntries(bioSamplesProperties.getBiosamplesClientCacheMaxEntries())
              .setMaxObjectSize(
                  bioSamplesProperties.getBiosamplesClientCacheMaxObjectSize()) // max size of
              // 1Mb
              // number of entries x size of entries = 1Gb total cache size
              .setSharedCache(false) // act like a browser cache not a middle-hop cache
              .build();

      // set a timeout limit
      final int timeout = bioSamplesProperties.getBiosamplesClientTimeout();
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
