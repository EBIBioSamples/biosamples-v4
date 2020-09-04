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
 * Java class for databaseType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="databaseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Name" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType"/&gt;
 *         &lt;element name="ID" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType" minOccurs="0"/&gt;
 *         &lt;element name="URI" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "databaseType",
    propOrder = {"name", "id", "uri"})
public class DatabaseType {

  @XmlElement(name = "Name", required = true)
  protected String name;

  @XmlElement(name = "ID")
  protected String id;

  @XmlElement(name = "URI")
  protected String uri;

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
   * Gets the value of the id property.
   *
   * @return possible object is {@link String }
   */
  public String getID() {
    return id;
  }

  /**
   * Sets the value of the id property.
   *
   * @param value allowed object is {@link String }
   */
  public void setID(String value) {
    this.id = value;
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
}
