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
public class EraproSample {
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

  Long getTaxId() {
    return taxId;
  }

  void setTaxId(final Long taxId) {
    this.taxId = taxId;
  }

  String getSampleXml() {
    return sampleXml;
  }

  void setSampleXml(final String sampleXml) {
    this.sampleXml = sampleXml;
  }

  String getFirstPublic() {
    return firstPublic;
  }

  void setFirstPublic(final String firstPublic) {
    this.firstPublic = firstPublic;
  }

  String getLastUpdate() {
    return lastUpdate;
  }

  void setLastUpdate(final String lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  String getFirstCreated() {
    return firstCreated;
  }

  void setFirstCreated(final String firstCreated) {
    this.firstCreated = firstCreated;
  }

  int getStatus() {
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
}
