package uk.ac.ebi.biosamples.model.ga4gh;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@JsonSerialize(using = AttributeSerializer.class)
@JsonDeserialize(using = AttributeDeserializer.class)
public class Attributes {
    private SortedMap<String, List<AttributeValue>> attributes;

    public Attributes() {
        this.attributes = new TreeMap<>();
    }


    public SortedMap<String, List<AttributeValue>> getAttributes() {
        return attributes;
    }

    public void setAttributes(SortedMap<String, List<AttributeValue>> attributes) {
        this.attributes = attributes;
    }

    void addAttribute(String label, List<AttributeValue> values) {
        try {
            attributes.put(label, values);
        }catch (NullPointerException e ){
            System.out.println();
        }
    }

    public void addSingleAttribute(String label, AttributeValue value) {
        ArrayList<AttributeValue> values = new ArrayList<>();
        values.add(value);
        attributes.put(label, values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attributes that = (Attributes) o;
        return Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {

        return Objects.hash(attributes);
    }

    @Override
    protected Attributes clone() {
        try {
            return (Attributes) super.clone();
        }catch (CloneNotSupportedException e){
            return new Attributes();
        }
    }
}
