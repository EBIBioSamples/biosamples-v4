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
package uk.ac.ebi.biosamples.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
public class Certificate implements Comparable<Certificate> {
  @JsonProperty("name")
  private String name;

  @JsonProperty("version")
  private String version;

  @JsonProperty("fileName")
  private String fileName;

  public Certificate(final String name, final String version, final String fileName) {
    this.name = name;
    this.version = version;
    this.fileName = fileName;
  }

  public Certificate() {}

  @Override
  public int compareTo(final Certificate cert) {
    if (cert == null) {
      return 1;
    }

    int comparison = nullSafeStringComparison(name, cert.name);

    if (comparison != 0) {
      return comparison;
    }

    comparison = nullSafeStringComparison(version, cert.version);
    if (comparison != 0) {
      return comparison;
    }

    return 0;
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

  @JsonCreator
  public static Certificate build(
      @JsonProperty("name") String name,
      @JsonProperty("version") String version,
      @JsonProperty("fileName") final String fileName) {
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

    final Certificate cert = new Certificate();
    cert.name = name;
    cert.fileName = fileName;
    cert.version = version;

    return cert;
  }
}
