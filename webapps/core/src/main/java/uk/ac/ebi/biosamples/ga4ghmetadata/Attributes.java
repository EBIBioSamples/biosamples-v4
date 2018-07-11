package uk.ac.ebi.biosamples.ga4ghmetadata;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.service.ga4ghService.*;

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
        attributes.put(label, values);
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
}
