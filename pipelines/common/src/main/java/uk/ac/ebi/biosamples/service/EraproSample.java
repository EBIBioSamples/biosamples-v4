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
package uk.ac.ebi.biosamples.service;

import uk.ac.ebi.biosamples.core.model.Sample;

/**
 * Bean to store all ENA sample related details fetched from ERAPRO for a {@link Sample}
 *
 * @author dgupta
 */
public class EraproSample {
  private String sampleXml;
  public String firstPublic;
  public String lastUpdated;
  private String firstCreated;
  public String brokerName;
  public String centreName;
  public String scientificName;
  public String commonName;
  private String submissionAccountId;
  public Long taxId;
  private int status;
  private String sampleId;
  public String biosampleId;
  private String biosampleAuthority;

  public String getSampleId() {
    return sampleId;
  }

  void setSampleId(final String sampleId) {
    this.sampleId = sampleId;
  }

  public String getBiosampleId() {
    return biosampleId;
  }

  void setBiosampleId(final String biosampleId) {
    this.biosampleId = biosampleId;
  }

  public String getBiosampleAuthority() {
    return biosampleAuthority;
  }

  void setBiosampleAuthority(final String biosampleAuthority) {
    this.biosampleAuthority = biosampleAuthority;
  }

  public Long getTaxId() {
    return taxId;
  }

  void setTaxId(final Long taxId) {
    this.taxId = taxId;
  }

  public String getSampleXml() {
    return sampleXml;
  }

  void setSampleXml(final String sampleXml) {
    this.sampleXml = sampleXml;
  }

  public String getFirstPublic() {
    return firstPublic;
  }

  void setFirstPublic(final String firstPublic) {
    this.firstPublic = firstPublic;
  }

  public String getLastUpdated() {
    return lastUpdated;
  }

  void setLastUpdated(final String lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public String getFirstCreated() {
    return firstCreated;
  }

  void setFirstCreated(final String firstCreated) {
    this.firstCreated = firstCreated;
  }

  public int getStatus() {
    return status;
  }

  void setStatus(final int status) {
    this.status = status;
  }

  public String getSubmissionAccountId() {
    return submissionAccountId;
  }

  void setSubmissionAccountId(final String submissionAccountId) {
    this.submissionAccountId = submissionAccountId;
  }

  public String getBrokerName() {
    return brokerName;
  }

  public EraproSample setBrokerName(final String brokerName) {
    this.brokerName = brokerName;
    return this;
  }

  public String getCentreName() {
    return centreName;
  }

  public EraproSample setCentreName(final String centreName) {
    this.centreName = centreName;
    return this;
  }

  public String getScientificName() {
    return scientificName;
  }

  public EraproSample setScientificName(final String scientificName) {
    this.scientificName = scientificName;
    return this;
  }

  public String getCommonName() {
    return commonName;
  }

  public void setCommonName(final String commonName) {
    this.commonName = commonName;
  }
}
