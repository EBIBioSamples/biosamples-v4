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
package uk.ac.ebi.biosamples.model.certification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class CertificationResult {
  private final List<Certificate> certificates = new ArrayList<>();
  private String sampleAccession;

  public CertificationResult(final String sampleAccession) {
    this.sampleAccession = sampleAccession;
  }

  private CertificationResult() {}

  @JsonIgnore
  public String getSampleAccession() {
    return sampleAccession;
  }

  public void add(final Certificate certificate) {
    certificates.add(certificate);
  }

  public List<Certificate> getCertificates() {
    return certificates;
  }
}
