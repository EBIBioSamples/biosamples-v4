
package uk.ac.ebi.biosamples.ebeye.gen;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for additional_fieldsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="additional_fieldsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="field" type="{}fieldType" maxOccurs="unbounded"/>
 *         &lt;element name="hierarchical_field" type="{}hierarchicalValueType" maxOccurs="unbounded"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "additional_fieldsType", propOrder = {
    "fieldOrHierarchicalField"
})
public class AdditionalFieldsType {

    @XmlElements({
        @XmlElement(name = "field", type = FieldType.class),
        @XmlElement(name = "hierarchical_field", type = HierarchicalValueType.class)
    })
    protected List<Object> fieldOrHierarchicalField;

    /**
     * Gets the value of the fieldOrHierarchicalField property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the fieldOrHierarchicalField property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFieldOrHierarchicalField().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FieldType }
     * {@link HierarchicalValueType }
     * 
     * 
     */
    public List<Object> getFieldOrHierarchicalField() {
        if (fieldOrHierarchicalField == null) {
            fieldOrHierarchicalField = new ArrayList<Object>();
        }
        return this.fieldOrHierarchicalField;
    }

}
