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
package uk.ac.ebi.biosamples.model.legacyxml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for organizationType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="organizationType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Name" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType"/&gt;
 *         &lt;element name="Address" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType" minOccurs="0"/&gt;
 *         &lt;element name="URI" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType" minOccurs="0"/&gt;
 *         &lt;element name="Email" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType" minOccurs="0"/&gt;
 *         &lt;element name="Role" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "organizationType",
    propOrder = {"name", "address", "uri", "email", "role"})
public class OrganizationType {

  @XmlElement(name = "Name", required = true)
  protected String name;

  @XmlElement(name = "Address")
  protected String address;

  @XmlElement(name = "URI")
  protected String uri;

  @XmlElement(name = "Email")
  protected String email;

  @XmlElement(name = "Role")
  protected String role;

  /**
   * Gets the value of the name property.
   *
   * @return possible object is {@link String }
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the value of the name property.
   *
   * @param value allowed object is {@link String }
   */
  public void setName(String value) {
    this.name = value;
  }

  /**
   * Gets the value of the address property.
   *
   * @return possible object is {@link String }
   */
  public String getAddress() {
    return address;
  }

  /**
   * Sets the value of the address property.
   *
   * @param value allowed object is {@link String }
   */
  public void setAddress(String value) {
    this.address = value;
  }

  /**
   * Gets the value of the uri property.
   *
   * @return possible object is {@link String }
   */
  public String getURI() {
    return uri;
  }

  /**
   * Sets the value of the uri property.
   *
   * @param value allowed object is {@link String }
   */
  public void setURI(String value) {
    this.uri = value;
  }

  /**
   * Gets the value of the email property.
   *
   * @return possible object is {@link String }
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the value of the email property.
   *
   * @param value allowed object is {@link String }
   */
  public void setEmail(String value) {
    this.email = value;
  }

  /**
   * Gets the value of the role property.
   *
   * @return possible object is {@link String }
   */
  public String getRole() {
    return role;
  }

  /**
   * Sets the value of the role property.
   *
   * @param value allowed object is {@link String }
   */
  public void setRole(String value) {
    this.role = value;
  }
}
