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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PipelinesProperties {
  @Value("${biosamples.pipelines.ncbi.file:/ncbi/biosample_set.xml.gz}")
  private String ncbiFile;

  @Value("${biosamples.pipelines.threadcount:1}")
  private int threadCount;

  @Value("${biosamples.pipelines.threadcount.max:60}")
  private int threadCountMax;

  @Value("${biosamples.pipelines.connectioncount.max:8}")
  private int connectionCountMax;

  @Value("${biosamples.pipelines.connectioncount.default:8}")
  private int connectionCountDefault;

  @Value("${biosamples.pipelines.connectioncount.ols:8}")
  private int connectionCountOls;

  @Value("${biosamples.pipelines.connectioncount.zooma:1}")
  private int connectionCountZooma;

  @Value("${biosamples.pipelines.connectiontimeout:60}")
  private int connectionTimeout;

  @Value("${biosamples.pipelines.zooma:https://www.ebi.ac.uk/spot/zooma}")
  private String zooma;

  @Value("${biosamples.pipelines.ncbi.domain:self.BiosampleImportNCBI}")
  private String ncbiDomain;

  @Value("${biosamples.pipelines.ncbi.controlledaccess:true}")
  private Boolean ncbiControlledAccess;

  @Value("${biosamples.pipelines.ena.domain:self.BiosampleImportENA}")
  private String enaDomain;

  @Value("${biosamples.pipelines.ena.webin_id:Webin-40894}")
  private String proxyWebinId;

  @Value("${biosamples.pipelines.accession.domain:self.BiosampleImportAcccession}")
  private String accessionDomain;

  @Value("${biosamples.pipelines.curation.domain:self.BiosampleCuration}")
  private String curationDomain;

  @Value("${biosamples.pipelines.zooma.domain:self.BiosampleZooma}")
  private String zoomaDomain;

  @Value("${biosamples.pipelines.copydown.domain:self.BiosampleCopydown}")
  private String copydownDomain;

  @Value("${biosamples.schemaValidator:http://localhost:3020/validate}")
  private String schemaValidator;

  @Value("${biosamples.schemaStore:http://localhost:8085/api/v2/schemas}")
  private String schemaStore;

  @Value(
      "${biosample.pipelines.samplerelease.get:http://wwwint.ebi.ac.uk/webin/era/service/test/biosample/release/}")
  private String webinEraServiceSampleReleaseGet;

  @Value(
      "${biosample.pipelines.samplerelease.delete:http://wwwint.ebi.ac.uk/webin/era/service/test/biosample/release/{biosampleAccession}}")
  private String webinEraServiceSampleReleaseDelete;

  public String getWebinEraServiceSampleReleaseGet() {
    return webinEraServiceSampleReleaseGet;
  }

  public String getWebinEraServiceSampleReleaseDelete() {
    return webinEraServiceSampleReleaseDelete;
  }

  public String getProxyWebinId() {
    return proxyWebinId;
  }

  public String getNcbiFile() {
    return ncbiFile;
  }

  public String getNcbiDomain() {
    return ncbiDomain;
  }

  public Boolean getNcbiControlledAccess() {
    return ncbiControlledAccess;
  }

  public String getEnaDomain() {
    return enaDomain;
  }

  public String getAccessionDomain() {
    return accessionDomain;
  }

  public String getCurationDomain() {
    return curationDomain;
  }

  public String getZoomaDomain() {
    return zoomaDomain;
  }

  public String getCopydownDomain() {
    return copydownDomain;
  }

  public int getThreadCount() {
    return threadCount;
  }

  public int getThreadCountMax() {
    return threadCountMax;
  }

  public int getConnectionCountMax() {
    return connectionCountMax;
  }

  public int getConnectionCountDefault() {
    return connectionCountDefault;
  }

  public int getConnectionCountOls() {
    return connectionCountOls;
  }

  public int getConnectionCountZooma() {
    return connectionCountZooma;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public String getZooma() {
    return zooma;
  }

  public String getSchemaValidator() {
    return schemaValidator;
  }

  public String getSchemaStore() {
    return schemaStore;
  }
}
