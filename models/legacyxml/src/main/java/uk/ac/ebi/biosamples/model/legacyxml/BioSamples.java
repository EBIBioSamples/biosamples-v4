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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="BioSampleGroup" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}bioSampleGroupType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="timestamp" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"bioSampleGroup"})
@XmlRootElement(name = "BioSamples")
public class BioSamples {

  @XmlElement(name = "BioSampleGroup")
  protected List<BioSampleGroupType> bioSampleGroup;

  @XmlAttribute(name = "timestamp")
  @XmlSchemaType(name = "unsignedLong")
  protected BigInteger timestamp;

  /**
   * Gets the value of the bioSampleGroup property.
   *
   * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any
   * modification you make to the returned list will be present inside the JAXB object. This is why
   * there is not a <CODE>set</CODE> method for the bioSampleGroup property.
   *
   * <p>For example, to add a new item, do as follows:
   *
   * <pre>
   *    getBioSampleGroup().add(newItem);
   * </pre>
   *
   * <p>Objects of the following type(s) are allowed in the list {@link BioSampleGroupType }
   */
  public List<BioSampleGroupType> getBioSampleGroup() {
    if (bioSampleGroup == null) {
      bioSampleGroup = new ArrayList<BioSampleGroupType>();
    }
    return this.bioSampleGroup;
  }

  /**
   * Gets the value of the timestamp property.
   *
   * @return possible object is {@link BigInteger }
   */
  public BigInteger getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the value of the timestamp property.
   *
   * @param value allowed object is {@link BigInteger }
   */
  public void setTimestamp(BigInteger value) {
    this.timestamp = value;
  }
}
