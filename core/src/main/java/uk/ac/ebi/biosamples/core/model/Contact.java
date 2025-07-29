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
@JsonDeserialize(builder = Contact.Builder.class)
public class Contact implements Comparable<Contact> {
  private final String firstName;
  private final String lastName;
  private final String midInitials;
  private final String role;
  private final String email;
  private final String affiliation;
  private final String name;
  private final String url;

  private Contact(
      final String firstName,
      final String lastName,
      final String midInitials,
      final String name,
      final String role,
      final String email,
      final String affiliation,
      final String url) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.midInitials = midInitials;
    this.name = name;
    this.role = role;
    this.email = email;
    this.affiliation = affiliation;
    this.url = url;
  }

  @JsonProperty("FirstName")
  public String getFirstName() {
    return firstName;
  }

  @JsonProperty("LastName")
  public String getLastName() {
    return lastName;
  }

  @JsonProperty("MidInitials")
  public String getMidInitials() {
    return midInitials;
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

  //    @JsonIgnore
  @JsonProperty("Affiliation")
  public String getAffiliation() {
    return affiliation;
  }

  //    @JsonIgnore
  @JsonProperty("URL")
  public String getUrl() {
    return url;
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }
    if (!(o instanceof Contact)) {
      return false;
    }
    final Contact other = (Contact) o;
    return Objects.equals(firstName, other.firstName)
        && Objects.equals(lastName, other.lastName)
        && Objects.equals(midInitials, other.midInitials)
        && Objects.equals(name, other.name)
        && Objects.equals(role, other.role)
        && Objects.equals(email, other.email)
        && Objects.equals(url, other.url)
        && Objects.equals(affiliation, other.affiliation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(firstName, lastName, midInitials, name, role, email, affiliation, url);
  }

  @Override
  public String toString() {
    return "Contact{"
        + "firstName='"
        + firstName
        + '\''
        + ", lastName='"
        + lastName
        + '\''
        + ", midInitials='"
        + midInitials
        + '\''
        + ", role='"
        + role
        + '\''
        + ", email='"
        + email
        + '\''
        + ", affiliation='"
        + affiliation
        + '\''
        + ", name='"
        + name
        + '\''
        + ", url='"
        + url
        + '\''
        + '}';
  }

  @Override
  public int compareTo(final Contact other) {
    if (other == null) {
      return 1;
    }

    int comparisonResult = nullSafeStringComparison(firstName, other.firstName);
    if (comparisonResult != 0) {
      return comparisonResult;
    }

    comparisonResult = nullSafeStringComparison(lastName, other.lastName);
    if (comparisonResult != 0) {
      return comparisonResult;
    }

    comparisonResult = nullSafeStringComparison(midInitials, other.midInitials);
    if (comparisonResult != 0) {
      return comparisonResult;
    }

    comparisonResult = nullSafeStringComparison(name, other.name);
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

    comparisonResult = nullSafeStringComparison(affiliation, other.affiliation);
    return comparisonResult;
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
  //	public static Contact build(@JsonProperty("Name") String name,
  //			@JsonProperty("Affiliation") String affiliation,
  //			@JsonProperty("URL") String url) {
  //		return new Contact(name, affiliation,url);
  //	}
  public static class Builder {
    private String firstName;
    private String lastName;
    private String midInitials;
    private String role;
    private String email;
    private String url;
    private String affiliation;
    private String name;

    @JsonCreator
    public Builder() {}

    @JsonProperty("FirstName")
    public Builder firstName(final String firstName) {
      this.firstName = firstName;
      return this;
    }

    @JsonProperty("LastName")
    public Builder lastName(final String lastName) {
      this.lastName = lastName;
      return this;
    }

    @JsonProperty("MidInitials")
    public Builder midInitials(final String midInitials) {
      this.midInitials = midInitials;
      return this;
    }

    @JsonProperty("Role")
    public Builder role(final String role) {
      this.role = role;
      return this;
    }

    @JsonProperty("E-mail")
    public Builder email(final String email) {
      this.email = email;
      return this;
    }

    @JsonProperty("URL")
    public Builder url(final String url) {
      this.url = url;
      return this;
    }

    @JsonProperty("Affiliation")
    public Builder affiliation(final String affiliation) {
      this.affiliation = affiliation;
      return this;
    }

    @JsonProperty("Name")
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public boolean isNotEmpty() {
      // only check fields that could be meaningful alone
      return StringUtils.hasText(firstName)
          || StringUtils.hasText(lastName)
          || StringUtils.hasText(email)
          || StringUtils.hasText(name)
          || StringUtils.hasText(url);
    }

    private String composedName() {
      final String nullSafeFirstName = firstName == null ? "" : firstName;
      final String nullSafeLastName = lastName == null ? "" : lastName;
      final String fullName = (nullSafeFirstName + " " + nullSafeLastName).trim();

      if (fullName.isEmpty()) {
        return null;
      }
      return fullName;
    }

    public Contact build() {
      if (name == null) {
        name = composedName();
      }
      return new Contact(firstName, lastName, midInitials, name, role, email, affiliation, url);
    }
  }
}
