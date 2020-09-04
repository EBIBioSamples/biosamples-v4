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
package uk.ac.ebi.biosamples.model.certification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import org.springframework.util.DigestUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SampleDocument {
  private String accession;
  private String document;
  private String hash;

  public SampleDocument(String accession, String document) {
    this.accession = accession;
    this.document = document;
  }

  public SampleDocument() {}

  public String getAccession() {
    return accession;
  }

  @JsonIgnore
  public String getDocument() {
    return document;
  }

  public void setAccession(String accession) {
    this.accession = accession;
  }

  public void setDocument(String document) {
    this.document = document;
  }

  @Override
  public String toString() {
    return "SampleDocument{"
        + "accession='"
        + accession
        + '\''
        + ", document='"
        + document
        + '\''
        + ", hash='"
        + hash
        + '\''
        + '}';
  }

  public String getHash() {
    if (hash == null) {
      this.hash = DigestUtils.md5DigestAsHex(this.document.getBytes()).toUpperCase();
    }
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SampleDocument sampleDocument = (SampleDocument) o;
    return accession.equals(sampleDocument.accession)
        && document.equals(sampleDocument.document)
        && hash.equals(sampleDocument.hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accession, document, hash);
  }
}
