
package uk.ac.ebi.biosamples.model.legacyxml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import uk.ac.ebi.biosamples.model.legacyxml.BioSample;
import uk.ac.ebi.biosamples.model.legacyxml.BioSampleGroup;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.ebi.ac.uk/biosamples/ResultQuery/1.0}SummaryInfo"/>
 *         &lt;element ref="{http://www.ebi.ac.uk/biosamples/ResultQuery/1.0}BioSample" maxOccurs="unbounded"/>
 *         &lt;element ref="{http://www.ebi.ac.uk/biosamples/ResultQuery/1.0}BioSampleGroup" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "summaryInfo",
    "bioSample",
    "bioSampleGroup"
})
@XmlRootElement(name = "ResultQuery")
public class ResultQuery {

    @XmlElement(name = "SummaryInfo", required = true)
    protected SummaryInfo summaryInfo;
    @XmlElement(name = "BioSampleGroup", required = false)
    protected List<BioSampleGroup> bioSampleGroup;
    @XmlElement(name = "BioSample", required = false)
    protected List<BioSample> bioSample;

    /**
     * Gets the value of the summaryInfo property.
     * 
     * @return
     *     possible object is
     *     {@link SummaryInfo }
     *     
     */
    public SummaryInfo getSummaryInfo() {
        return summaryInfo;
    }

    /**
     * Sets the value of the summaryInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link SummaryInfo }
     *     
     */
    public void setSummaryInfo(SummaryInfo value) {
        this.summaryInfo = value;
    }
    
    /**
     * Gets the value of the bioSample property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the bioSample property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBioSample().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BioSample }
     * 
     * 
     */
    public List<BioSample> getBioSample() {
        if (bioSample == null) {
            bioSample = new ArrayList<BioSample>();
        }
        return this.bioSample;
    }


    /**
     * Gets the value of the bioSampleGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the bioSampleGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBioSampleGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BioSampleGroup }
     * 
     * 
     */
    public List<BioSampleGroup> getBioSampleGroup() {
        if (bioSampleGroup == null) {
            bioSampleGroup = new ArrayList<BioSampleGroup>();
        }
        return this.bioSampleGroup;
    }

}
