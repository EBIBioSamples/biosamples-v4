
package uk.ac.ebi.biosamples.ebeye.gen;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for refType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="refType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="dbname" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="dbkey" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "refType")
public class RefType {

    @XmlAttribute(name = "dbname", required = true)
    protected String dbname;
    @XmlAttribute(name = "dbkey", required = true)
    protected String dbkey;

    /**
     * Gets the value of the dbname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDbname() {
        return dbname;
    }

    /**
     * Sets the value of the dbname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDbname(String value) {
        this.dbname = value;
    }

    /**
     * Gets the value of the dbkey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDbkey() {
        return dbkey;
    }

    /**
     * Sets the value of the dbkey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDbkey(String value) {
        this.dbkey = value;
    }

}
