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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for propertyType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="propertyType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="QualifiedValue" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}qualifiedValueType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="class" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="characteristic" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="comment" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "propertyType",
    propOrder = {"qualifiedValue"})
public class PropertyType {

  @XmlElement(name = "QualifiedValue")
  protected List<QualifiedValueType> qualifiedValue;

  @XmlAttribute(name = "class", required = true)
  protected String clazz;

  @XmlAttribute(name = "characteristic")
  protected Boolean characteristic;

  @XmlAttribute(name = "comment")
  protected Boolean comment;

  @XmlAttribute(name = "type")
  protected String type;

  /**
   * Gets the value of the qualifiedValue property.
   *
   * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any
   * modification you make to the returned list will be present inside the JAXB object. This is why
   * there is not a <CODE>set</CODE> method for the qualifiedValue property.
   *
   * <p>For example, to add a new item, do as follows:
   *
   * <pre>
   *    getQualifiedValue().add(newItem);
   * </pre>
   *
   * <p>Objects of the following type(s) are allowed in the list {@link QualifiedValueType }
   */
  public List<QualifiedValueType> getQualifiedValue() {
    if (qualifiedValue == null) {
      qualifiedValue = new ArrayList<QualifiedValueType>();
    }
    return this.qualifiedValue;
  }

  /**
   * Gets the value of the clazz property.
   *
   * @return possible object is {@link String }
   */
  public String getClazz() {
    return clazz;
  }

  /**
   * Sets the value of the clazz property.
   *
   * @param value allowed object is {@link String }
   */
  public void setClazz(String value) {
    this.clazz = value;
  }

  /**
   * Gets the value of the characteristic property.
   *
   * @return possible object is {@link Boolean }
   */
  public Boolean isCharacteristic() {
    return characteristic;
  }

  /**
   * Sets the value of the characteristic property.
   *
   * @param value allowed object is {@link Boolean }
   */
  public void setCharacteristic(Boolean value) {
    this.characteristic = value;
  }

  /**
   * Gets the value of the comment property.
   *
   * @return possible object is {@link Boolean }
   */
  public Boolean isComment() {
    return comment;
  }

  /**
   * Sets the value of the comment property.
   *
   * @param value allowed object is {@link Boolean }
   */
  public void setComment(Boolean value) {
    this.comment = value;
  }

  /**
   * Gets the value of the type property.
   *
   * @return possible object is {@link String }
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the value of the type property.
   *
   * @param value allowed object is {@link String }
   */
  public void setType(String value) {
    this.type = value;
  }
}
