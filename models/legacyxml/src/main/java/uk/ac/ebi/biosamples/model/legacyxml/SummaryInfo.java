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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.ebi.ac.uk/biosamples/ResultQuery/1.0}Total"/>
 *         &lt;element ref="{http://www.ebi.ac.uk/biosamples/ResultQuery/1.0}From"/>
 *         &lt;element ref="{http://www.ebi.ac.uk/biosamples/ResultQuery/1.0}To"/>
 *         &lt;element ref="{http://www.ebi.ac.uk/biosamples/ResultQuery/1.0}PageNumber"/>
 *         &lt;element ref="{http://www.ebi.ac.uk/biosamples/ResultQuery/1.0}PageSize"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"total", "from", "to", "pageNumber", "pageSize"})
@XmlRootElement(name = "SummaryInfo")
public class SummaryInfo {

  @XmlElement(name = "Total", required = true)
  protected BigInteger total;

  @XmlElement(name = "From", required = true)
  protected BigInteger from;

  @XmlElement(name = "To", required = true)
  protected BigInteger to;

  @XmlElement(name = "PageNumber", required = true)
  protected BigInteger pageNumber;

  @XmlElement(name = "PageSize", required = true)
  protected BigInteger pageSize;

  /**
   * Gets the value of the total property.
   *
   * @return possible object is {@link BigInteger }
   */
  public BigInteger getTotal() {
    return total;
  }

  /**
   * Sets the value of the total property.
   *
   * @param value allowed object is {@link BigInteger }
   */
  public void setTotal(BigInteger value) {
    this.total = value;
  }

  /**
   * Gets the value of the from property.
   *
   * @return possible object is {@link BigInteger }
   */
  public BigInteger getFrom() {
    return from;
  }

  /**
   * Sets the value of the from property.
   *
   * @param value allowed object is {@link BigInteger }
   */
  public void setFrom(BigInteger value) {
    this.from = value;
  }

  /**
   * Gets the value of the to property.
   *
   * @return possible object is {@link BigInteger }
   */
  public BigInteger getTo() {
    return to;
  }

  /**
   * Sets the value of the to property.
   *
   * @param value allowed object is {@link BigInteger }
   */
  public void setTo(BigInteger value) {
    this.to = value;
  }

  /**
   * Gets the value of the pageNumber property.
   *
   * @return possible object is {@link BigInteger }
   */
  public BigInteger getPageNumber() {
    return pageNumber;
  }

  /**
   * Sets the value of the pageNumber property.
   *
   * @param value allowed object is {@link BigInteger }
   */
  public void setPageNumber(BigInteger value) {
    this.pageNumber = value;
  }

  /**
   * Gets the value of the pageSize property.
   *
   * @return possible object is {@link BigInteger }
   */
  public BigInteger getPageSize() {
    return pageSize;
  }

  /**
   * Sets the value of the pageSize property.
   *
   * @param value allowed object is {@link BigInteger }
   */
  public void setPageSize(BigInteger value) {
    this.pageSize = value;
  }
}
