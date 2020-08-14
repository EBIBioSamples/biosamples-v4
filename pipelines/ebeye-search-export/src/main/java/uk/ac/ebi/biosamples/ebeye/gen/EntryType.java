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
package uk.ac.ebi.biosamples.ebeye.gen;

import javax.xml.bind.annotation.*;

/**
 * Java class for entryType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="entryType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="authors" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="keywords" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="dates" type="{}datesType" minOccurs="0"/>
 *         &lt;element name="cross_references" type="{}cross_referencesType" minOccurs="0"/>
 *         &lt;element name="additional_fields" type="{}additional_fieldsType" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="acc" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlRootElement(name = "entry")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "entryType",
    propOrder = {})
public class EntryType {

  protected String name;
  protected String description;
  protected String authors;
  protected String keywords;
  protected DatesType dates;

  @XmlElement(name = "cross_references")
  protected CrossReferencesType crossReferences;

  @XmlElement(name = "additional_fields")
  protected AdditionalFieldsType additionalFields;

  @XmlAttribute(name = "id", required = true)
  protected String id;

  @XmlAttribute(name = "acc")
  protected String acc;

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
   * Gets the value of the description property.
   *
   * @return possible object is {@link String }
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the value of the description property.
   *
   * @param value allowed object is {@link String }
   */
  public void setDescription(String value) {
    this.description = value;
  }

  /**
   * Gets the value of the authors property.
   *
   * @return possible object is {@link String }
   */
  public String getAuthors() {
    return authors;
  }

  /**
   * Sets the value of the authors property.
   *
   * @param value allowed object is {@link String }
   */
  public void setAuthors(String value) {
    this.authors = value;
  }

  /**
   * Gets the value of the keywords property.
   *
   * @return possible object is {@link String }
   */
  public String getKeywords() {
    return keywords;
  }

  /**
   * Sets the value of the keywords property.
   *
   * @param value allowed object is {@link String }
   */
  public void setKeywords(String value) {
    this.keywords = value;
  }

  /**
   * Gets the value of the dates property.
   *
   * @return possible object is {@link DatesType }
   */
  public DatesType getDates() {
    return dates;
  }

  /**
   * Sets the value of the dates property.
   *
   * @param value allowed object is {@link DatesType }
   */
  public void setDates(DatesType value) {
    this.dates = value;
  }

  /**
   * Gets the value of the crossReferences property.
   *
   * @return possible object is {@link CrossReferencesType }
   */
  public CrossReferencesType getCrossReferences() {
    return crossReferences;
  }

  /**
   * Sets the value of the crossReferences property.
   *
   * @param value allowed object is {@link CrossReferencesType }
   */
  public void setCrossReferences(CrossReferencesType value) {
    this.crossReferences = value;
  }

  /**
   * Gets the value of the additionalFields property.
   *
   * @return possible object is {@link AdditionalFieldsType }
   */
  public AdditionalFieldsType getAdditionalFields() {
    return additionalFields;
  }

  /**
   * Sets the value of the additionalFields property.
   *
   * @param value allowed object is {@link AdditionalFieldsType }
   */
  public void setAdditionalFields(AdditionalFieldsType value) {
    this.additionalFields = value;
  }

  /**
   * Gets the value of the id property.
   *
   * @return possible object is {@link String }
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the value of the id property.
   *
   * @param value allowed object is {@link String }
   */
  public void setId(String value) {
    this.id = value;
  }

  /**
   * Gets the value of the acc property.
   *
   * @return possible object is {@link String }
   */
  public String getAcc() {
    return acc;
  }

  /**
   * Sets the value of the acc property.
   *
   * @param value allowed object is {@link String }
   */
  public void setAcc(String value) {
    this.acc = value;
  }
}
