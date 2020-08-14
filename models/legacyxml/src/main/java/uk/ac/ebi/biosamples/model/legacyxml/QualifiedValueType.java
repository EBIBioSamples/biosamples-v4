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
 * Java class for qualifiedValueType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="qualifiedValueType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Value" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType"/&gt;
 *         &lt;element name="TermSourceREF" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}termSourceREFType" minOccurs="0"/&gt;
 *         &lt;element name="Unit" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "qualifiedValueType",
    propOrder = {"value", "termSourceREF", "unit"})
public class QualifiedValueType {

  @XmlElement(name = "Value", required = true)
  protected String value;

  @XmlElement(name = "TermSourceREF")
  protected TermSourceREFType termSourceREF;

  @XmlElement(name = "Unit")
  protected String unit;

  /**
   * Gets the value of the value property.
   *
   * @return possible object is {@link String }
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value of the value property.
   *
   * @param value allowed object is {@link String }
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Gets the value of the termSourceREF property.
   *
   * @return possible object is {@link TermSourceREFType }
   */
  public TermSourceREFType getTermSourceREF() {
    return termSourceREF;
  }

  /**
   * Sets the value of the termSourceREF property.
   *
   * @param value allowed object is {@link TermSourceREFType }
   */
  public void setTermSourceREF(TermSourceREFType value) {
    this.termSourceREF = value;
  }

  /**
   * Gets the value of the unit property.
   *
   * @return possible object is {@link String }
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Sets the value of the unit property.
   *
   * @param value allowed object is {@link String }
   */
  public void setUnit(String value) {
    this.unit = value;
  }
}
