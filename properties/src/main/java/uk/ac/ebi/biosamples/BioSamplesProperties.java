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
package uk.ac.ebi.biosamples;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BioSamplesProperties {

  @Value("${biosamples.agent.solr.stayalive:false}")
  private Boolean agentSolrStayalive;

  @Value("${biosamples.client.uri:http://localhost:8081}")
  private URI biosamplesClientUri;

  @Value("${biosamples.client.pagesize:1000}")
  private int biosamplesClientPagesize;

  // in milliseconds
  @Value("${biosamples.client.timeout:60000}")
  private int biosamplesClientTimeout;

  @Value("${biosamples.client.connectioncount.max:8}")
  private int connectionCountMax;

  @Value("${biosamples.client.connectioncount.default:8}")
  private int connectionCountDefault;

  @Value("${biosamples.client.threadcount:1}")
  private int threadCount;

  @Value("${biosamples.client.threadcount.max:8}")
  private int threadCountMax;

  @Value("${biosamples.client.aap.uri:https://explore.api.aai.ebi.ac.uk/auth}")
  private URI biosamplesClientAapUri;

  // can't use "null" because it will be a string
  @Value("${biosamples.client.aap.username:#{null}}")
  private String biosamplesClientAapUsername;

  // can't use "null" because it will be a string
  @Value("${biosamples.client.aap.password:#{null}}")
  private String biosamplesClientAapPassword;

  // max number of cache entries, 0 means no cache is used by the client
  // This multiplied by the cache maxobjectsize value defines the max size of the cache
  @Value("${biosamples.client.cache.maxentries:0}")
  private int biosamplesClientCacheMaxEntries;

  // Set each cache object maximum size, 1024*1024 = 1048576 = 1Mb
  @Value("${biosamples.client.cache.maxobjectsize:1048576}")
  private int biosamplesClientCacheMaxObjectSize;

  @Value("${biosamples.aap.super.read:self.BiosampleSuperUserRead}")
  private String biosamplesAapSuperRead;

  @Value("${biosamples.aap.super.write:self.BiosampleSuperUserWrite}")
  private String biosamplesAapSuperWrite;

  @Value("${biosamples.ols:https://www.ebi.ac.uk/ols}")
  private String ols;

  @Value("${biosamples.webapp.sampletab.uri:http://localhost:8082/biosamples/sampletab}")
  private URI biosamplesWebappSampletabUri;

  @Value("${biosamples.webapp.core.uri:http://localhost:8081/biosamples}")
  private URI biosamplesWebappCoreUri;

  @Value("${biosamples.usi.core.uri:https://submission-dev.ebi.ac.uk/api/docs/guide_overview.html}")
  private URI usiCoreUri;

  @Value("${biosamples.webapp.core.page.threadcount:64}")
  private int webappCorePageThreadCount;

  @Value("${biosamples.webapp.core.page.threadcount.max:128}")
  private int webappCorePageThreadCountMax;

  // in seconds
  @Value("${biosamples.webapp.core.page.cache.maxage:300}")
  private int webappCorePageCacheMaxAge;

  // cache facets upto 24 hours (in seconds)
  @Value("${biosamples.webapp.core.facet.cache.maxage:86400}")
  private int webappCoreFacetCacheMaxAge;

  @Value("${biosamples.schema.validator.uri:http://localhost:8085/validate}")
  private URI biosamplesSchemaValidatorServiceUri;

  public URI getBiosamplesClientUri() {
    return biosamplesClientUri;
  }

  public int getBiosamplesClientPagesize() {
    return biosamplesClientPagesize;
  }

  public int getBiosamplesClientTimeout() {
    return biosamplesClientTimeout;
  }

  public int getBiosamplesClientConnectionCountMax() {
    return connectionCountMax;
  }

  public int getBiosamplesClientThreadCount() {
    return threadCount;
  }

  public int getBiosamplesClientThreadCountMax() {
    return threadCountMax;
  }

  public int getBiosamplesClientConnectionCountDefault() {
    return connectionCountDefault;
  }

  public URI getBiosamplesClientAapUri() {
    return biosamplesClientAapUri;
  }

  public String getBiosamplesClientAapUsername() {
    return biosamplesClientAapUsername;
  }

  public String getBiosamplesClientAapPassword() {
    return biosamplesClientAapPassword;
  }

  public int getBiosamplesClientCacheMaxEntries() {
    return biosamplesClientCacheMaxEntries;
  }

  public int getBiosamplesClientCacheMaxObjectSize() {
    return biosamplesClientCacheMaxObjectSize;
  }

  public String getBiosamplesAapSuperRead() {
    return biosamplesAapSuperRead;
  }

  public String getBiosamplesAapSuperWrite() {
    return biosamplesAapSuperWrite;
  }

  public boolean getAgentSolrStayalive() {
    return agentSolrStayalive;
  }

  public String getOls() {
    return ols;
  }

  public int getBiosamplesCorePageThreadCount() {
    return webappCorePageThreadCount;
  }

  public int getBiosamplesCorePageThreadCountMax() {
    return webappCorePageThreadCountMax;
  }

  public int getBiosamplesCorePageCacheMaxAge() {
    return webappCorePageCacheMaxAge;
  }

  public int getBiosamplesCoreFacetCacheMaxAge() {
    return webappCoreFacetCacheMaxAge;
  }

  public URI getBiosamplesWebappCoreUri() {
    return biosamplesWebappCoreUri;
  }

  public URI getBiosamplesWebappSampletabUri() {
    return biosamplesWebappSampletabUri;
  }

  public URI getUsiCoreUri() {
    return usiCoreUri;
  }

  public URI getBiosamplesSchemaValidatorServiceUri() {
    return biosamplesSchemaValidatorServiceUri;
  }
}
