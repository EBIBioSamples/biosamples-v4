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
package uk.ac.ebi.biosamples.mongo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import uk.ac.ebi.biosamples.model.Certificate;

public class MongoCertificate implements Comparable<MongoCertificate> {
  private String name;
  private String version;
  private String fileName;

  public MongoCertificate(final String name, final String version, final String fileName) {
    this.name = name;
    this.version = version;
    this.fileName = fileName;
  }

  private MongoCertificate() {}

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @JsonProperty("version")
  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  @JsonProperty("fileName")
  public String getFileName() {
    return fileName;
  }

  public void setFileName(final String fileName) {
    this.fileName = fileName;
  }

  @Override
  public String toString() {
    return "Certificate{"
        + "name='"
        + name
        + '\''
        + ", version='"
        + version
        + '\''
        + ", fileName='"
        + fileName
        + '\''
        + '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Certificate)) {
      return false;
    }
    final Certificate that = (Certificate) o;
    return Objects.equals(getName(), that.getName())
        && Objects.equals(getVersion(), that.getVersion())
        && Objects.equals(getFileName(), that.getFileName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getVersion(), getFileName());
  }

  @Override
  public int compareTo(final MongoCertificate cert) {
    if (cert == null) {
      return 1;
    }

    int comparison = nullSafeStringComparison(name, cert.name);

    if (comparison != 0) {
      return comparison;
    }

    comparison = nullSafeStringComparison(version, cert.version);
    return comparison;
  }

  private int nullSafeStringComparison(final String one, final String two) {
    if (one == null && two != null) {
      return -1;
    }
    if (one != null && two == null) {
      return 1;
    }
    if (one != null && !one.equals(two)) {
      return one.compareTo(two);
    }

    return 0;
  }

  public static MongoCertificate build(String name, String version, final String fileName) {
    // check for nulls
    if (name == null) {
      throw new IllegalArgumentException("Certificate name must not be null");
    }

    if (version == null) {
      version = "";
    }

    if (fileName == null) {
      throw new IllegalArgumentException("Certificate file name must not be null");
    }

    name = name.trim();
    version = version.trim();

    final MongoCertificate cert = new MongoCertificate();
    cert.name = name;
    cert.fileName = fileName;
    cert.version = version;

    return cert;
  }
}
