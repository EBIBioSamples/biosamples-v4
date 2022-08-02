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
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class PipelinesHelper {

  public RestTemplateCustomizer getRestTemplateCustomizer(
      BioSamplesProperties bioSamplesProperties, PipelinesProperties pipelinesProperties) {
    return restTemplate -> {
      HttpClient httpClient = configureHttpClient(pipelinesProperties, bioSamplesProperties);
      restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    };
  }

  private HttpClient configureHttpClient(
      PipelinesProperties pipelinesProperties, BioSamplesProperties bioSamplesProperties) {
    ConnectionKeepAliveStrategy keepAliveStrategy = configureConnectionKeepAliveStrategy();
    PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
        configurePoolingHttpClientConnectionManager(pipelinesProperties, bioSamplesProperties);
    CacheConfig cacheConfig = configureCache();
    RequestConfig config = configureTimeout(pipelinesProperties.getConnectionTimeout());

    return CachingHttpClientBuilder.create()
        .setCacheConfig(cacheConfig)
        .useSystemProperties()
        .setConnectionManager(poolingHttpClientConnectionManager)
        .setKeepAliveStrategy(keepAliveStrategy)
        .setDefaultRequestConfig(config)
        .build();
  }

  private RequestConfig configureTimeout(int timeout) {
    return RequestConfig.custom()
        .setConnectTimeout(timeout * 1000) // time to establish the connection with the remote host
        .setConnectionRequestTimeout(
            timeout * 1000) // maximum time of inactivity between two data packets
        .setSocketTimeout(timeout * 1000)
        .build();
  }

  private ConnectionKeepAliveStrategy configureConnectionKeepAliveStrategy() {
    return (response, context) -> {
      long keepAliveDuration = 60 * 1000L;

      // check if there is a non-standard keep alive header present
      HeaderElementIterator it =
          new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
      while (it.hasNext()) {
        HeaderElement he = it.nextElement();
        String param = he.getName();
        String value = he.getValue();
        if (value != null && param.equalsIgnoreCase("timeout")) {
          keepAliveDuration = Long.parseLong(value) * 1000;
        }
      }

      return keepAliveDuration;
    };
  }

  private PoolingHttpClientConnectionManager configurePoolingHttpClientConnectionManager(
      PipelinesProperties pipelinesProperties, BioSamplesProperties bioSamplesProperties) {

    PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
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

    return poolingHttpClientConnectionManager;
  }

  private CacheConfig configureCache() {
    return CacheConfig.custom()
        .setMaxCacheEntries(1024)
        .setMaxObjectSize(1024 * 1024L) // max size of 1Mb
        // number of entries x size of entries = 1Gb total cache size
        .setSharedCache(false) // act like a browser cache not a middle-hop cache
        .build();
  }
}
