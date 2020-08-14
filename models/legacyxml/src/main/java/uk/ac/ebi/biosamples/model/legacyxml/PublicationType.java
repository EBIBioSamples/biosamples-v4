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
 * Java class for publicationType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="publicationType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="DOI" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType" minOccurs="0"/&gt;
 *         &lt;element name="PubMedID" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "publicationType",
    propOrder = {"doi", "pubMedID"})
public class PublicationType {

  @XmlElement(name = "DOI")
  protected String doi;

  @XmlElement(name = "PubMedID")
  protected String pubMedID;

  /**
   * Gets the value of the doi property.
   *
   * @return possible object is {@link String }
   */
  public String getDOI() {
    return doi;
  }

  /**
   * Sets the value of the doi property.
   *
   * @param value allowed object is {@link String }
   */
  public void setDOI(String value) {
    this.doi = value;
  }

  /**
   * Gets the value of the pubMedID property.
   *
   * @return possible object is {@link String }
   */
  public String getPubMedID() {
    return pubMedID;
  }

  /**
   * Sets the value of the pubMedID property.
   *
   * @param value allowed object is {@link String }
   */
  public void setPubMedID(String value) {
    this.pubMedID = value;
  }
}
