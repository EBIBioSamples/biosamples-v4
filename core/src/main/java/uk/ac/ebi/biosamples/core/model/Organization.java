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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;
import org.springframework.util.StringUtils;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = Organization.Builder.class)
public class Organization implements Comparable<Organization> {
  private final String name;
  private final String role;
  private final String address;
  private final String email;
  private final String url;

  private Organization(
      final String name,
      final String role,
      final String email,
      final String url,
      final String address) {
    this.name = name;
    this.role = role;
    this.email = email;
    this.url = url;
    this.address = address;
  }

  @JsonProperty("Name")
  public String getName() {
    return name;
  }

  @JsonProperty("Role")
  public String getRole() {
    return role;
  }

  @JsonProperty("E-mail")
  public String getEmail() {
    return email;
  }

  @JsonProperty("URL")
  public String getUrl() {
    return url;
  }

  @JsonProperty("Address")
  public String getAddress() {
    return address;
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }
    if (!(o instanceof Organization)) {
      return false;
    }
    final Organization other = (Organization) o;
    return Objects.equals(name, other.name)
        && Objects.equals(role, other.role)
        && Objects.equals(email, other.email)
        && Objects.equals(url, other.url)
        && Objects.equals(address, other.address);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, role, email, url, address);
  }

  @Override
  public int compareTo(final Organization other) {
    if (other == null) {
      return 1;
    }

    int comparisonResult = nullSafeStringComparison(name, other.name);
    if (comparisonResult != 0) {
      return comparisonResult;
    }

    comparisonResult = nullSafeStringComparison(address, other.address);
    if (comparisonResult != 0) {
      return comparisonResult;
    }

    comparisonResult = nullSafeStringComparison(role, other.role);
    if (comparisonResult != 0) {
      return comparisonResult;
    }

    comparisonResult = nullSafeStringComparison(email, other.email);
    if (comparisonResult != 0) {
      return comparisonResult;
    }

    return nullSafeStringComparison(url, other.url);
  }

  @Override
  public String toString() {
    return "Organization{"
        + "name='"
        + name
        + '\''
        + ", role='"
        + role
        + '\''
        + ", address='"
        + address
        + '\''
        + ", email='"
        + email
        + '\''
        + ", url='"
        + url
        + '\''
        + '}';
  }

  private int nullSafeStringComparison(final String first, final String other) {
    if (first == null && other == null) {
      return 0;
    }
    if (first == null) {
      return -1;
    }
    if (other == null) {
      return 1;
    }
    return first.compareTo(other);
  }

  //	@JsonCreator
  //	public static Organization build(@JsonProperty("Name") String name,
  //			@JsonProperty("Role") String role,
  //			@JsonProperty("E-mail") String email,
  //			@JsonProperty("URL") String url) {
  //		return new Organization(name, role,email,url);
  //	}
  public static class Builder {
    private String name;
    private String url;
    private String email;
    private String address;
    private String role;

    @JsonCreator
    public Builder() {}

    @JsonProperty("Name")
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    @JsonProperty("URL")
    public Builder url(final String url) {
      this.url = url;
      return this;
    }

    @JsonProperty("E-mail")
    public Builder email(final String email) {
      this.email = email;
      return this;
    }

    @JsonProperty("Address")
    public Builder address(final String address) {
      this.address = address;
      return this;
    }

    @JsonProperty("Role")
    public Builder role(final String role) {
      this.role = role;
      return this;
    }

    public boolean isNotEmpty() {
      // only check fields that could be meaningful alone
      return StringUtils.hasText(name)
          || StringUtils.hasText(url)
          || StringUtils.hasText(email)
          || StringUtils.hasText(address);
    }

    public Organization build() {
      return new Organization(name, role, email, url, address);
    }
  }
}
