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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.configuration.ExclusionConfiguration;
import uk.ac.ebi.biosamples.security.service.BioSamplesWebSecurityConfig;
import uk.ac.ebi.biosamples.service.EnaConfig;
import uk.ac.ebi.biosamples.service.EnaSampleToBioSampleConversionService;
import uk.ac.ebi.biosamples.service.EraProDao;
import uk.ac.ebi.biosamples.utils.PipelineUtils;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@ComponentScan(
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          value = {
            EnaConfig.class,
            EraProDao.class,
            EnaSampleToBioSampleConversionService.class,
            BioSamplesWebSecurityConfig.class
          }),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "uk\\.ac\\.ebi\\.biosamples\\.service\\.validation\\..*")
    })
@Import(ExclusionConfiguration.class)
public class Application {

  public static void main(final String[] args) {
    final ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
    PipelineUtils.exitPipeline(ctx);
  }

  // todo I Had to add restTemplate bean as a temporary workaround as there seems to be a problem
  // with dependencies after refactor.
  //  We need to sort out dependency problem and remove this unused dependency.

  @Bean
  public RestTemplate restTemplate(final RestTemplateCustomizer restTemplateCustomizer) {
    final RestTemplate restTemplate = new RestTemplate();
    restTemplateCustomizer.customize(restTemplate);
    return restTemplate;
  }

  @Bean
  public RestTemplateCustomizer restTemplateCustomizer(
      final BioSamplesProperties bioSamplesProperties,
      final PipelinesProperties pipelinesProperties) {
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
      poolingHttpClientConnectionManager.setMaxTotal(pipelinesProperties.getConnectionCountMax());
      poolingHttpClientConnectionManager.setDefaultMaxPerRoute(
          pipelinesProperties.getConnectionCountDefault());
      poolingHttpClientConnectionManager.setMaxPerRoute(
          new HttpRoute(HttpHost.create(pipelinesProperties.getZooma())),
          pipelinesProperties.getConnectionCountZooma());
      poolingHttpClientConnectionManager.setMaxPerRoute(
          new HttpRoute(HttpHost.create(bioSamplesProperties.getOls())),
          pipelinesProperties.getConnectionCountOls());

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
              .setConnectionRequestTimeout(timeout * 1000)
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
