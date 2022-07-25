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
package uk.ac.ebi.biosamples.ena;

import uk.ac.ebi.biosamples.model.Sample;

/**
 * Bean to store all ENA sample related details fetched from ERAPRO for a {@link Sample}
 *
 * @author dgupta
 */
public class SampleDBBean {
  private String sampleXml;
  private String firstPublic;
  private String lastUpdate;
  private String firstCreated;
  private String submissionAccountId;
  private Long taxId;
  private int status;
  private String sampleId;
  private String biosampleId;
  private String biosampleAuthority;

  public String getSampleId() {
    return sampleId;
  }

  public void setSampleId(String sampleId) {
    this.sampleId = sampleId;
  }

  public String getBiosampleId() {
    return biosampleId;
  }

  public void setBiosampleId(String biosampleId) {
    this.biosampleId = biosampleId;
  }

  public String getBiosampleAuthority() {
    return biosampleAuthority;
  }

  public void setBiosampleAuthority(String biosampleAuthority) {
    this.biosampleAuthority = biosampleAuthority;
  }

  public Long getTaxId() {
    return taxId;
  }

  public void setTaxId(Long taxId) {
    this.taxId = taxId;
  }

  public String getSampleXml() {
    return sampleXml;
  }

  public void setSampleXml(String sampleXml) {
    this.sampleXml = sampleXml;
  }

  public String getFirstPublic() {
    return firstPublic;
  }

  public void setFirstPublic(String firstPublic) {
    this.firstPublic = firstPublic;
  }

  public String getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(String lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public String getFirstCreated() {
    return firstCreated;
  }

  public void setFirstCreated(String firstCreated) {
    this.firstCreated = firstCreated;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getSubmissionAccountId() {
    return submissionAccountId;
  }

  public void setSubmissionAccountId(String submissionAccountId) {
    this.submissionAccountId = submissionAccountId;
  }
}
