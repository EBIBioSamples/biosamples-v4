package uk.ac.ebi.biosamples.model.ga4gh;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@JsonSerialize(using = AttributeSerializer.class)
@JsonDeserialize(using = AttributeDeserializer.class)
public class Ga4ghAttributes {
    private SortedMap<String, List<Ga4ghAttributeValue>> attributes;

    public Ga4ghAttributes() {
        this.attributes = new TreeMap<>();
    }


    public SortedMap<String, List<Ga4ghAttributeValue>> getAttributes() {
        return attributes;
    }

    public void setAttributes(SortedMap<String, List<Ga4ghAttributeValue>> attributes) {
        this.attributes = attributes;
    }

    void addAttribute(String label, List<Ga4ghAttributeValue> values) {
        attributes.put(label, values);
    }

    public void addSingleAttribute(String label, Ga4ghAttributeValue value) {
        ArrayList<Ga4ghAttributeValue> values = new ArrayList<>();
        values.add(value);
        attributes.put(label, values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ga4ghAttributes that = (Ga4ghAttributes) o;
        return Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {

        return Objects.hash(attributes);
    }
}
