package uk.ac.ebi.biosamples.model;

import org.junit.Test;
import uk.ac.ebi.biosamples.model.ga4gh.*;

import java.util.LinkedList;

import static org.junit.Assert.*;

public class AttributeValueTest {
    @Test
    public void string_value_test() {
        String value = "value";
        AttributeValue val1 = new AttributeValue(value);
        AttributeValue val2 = new AttributeValue("");
        val2.setType("string_value");
        val2.setValue("value");
        assertEquals(val1.getType(), val2.getType());
    }

//    @Test
//    public void int64_value_test() {
//        Long value = 1L;
//        AttributeValue val1 = new AttributeValue(value);
//        AttributeValue val2 = new AttributeValue(0);
//        val2.setType("int64_value");
//        val2.setValue(1);
//        assertEquals(val1.getType(), val2.getType());
//    }

    @Test
    public void bool_value_test() {
        Boolean value = true;
        AttributeValue val1 = new AttributeValue(value);
        AttributeValue val2 = new AttributeValue(false);
        val2.setType("bool_value");
        val2.setValue(true);
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void double_value_test() {
        Double value = 1.11111;
        AttributeValue val1 = new AttributeValue(value);
        AttributeValue val2 = new AttributeValue(0.111);
        val2.setType("double_value");
        val2.setValue(1.11111);
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void external_identifier_value_test() {
        ExternalIdentifier value = new ExternalIdentifier();
        value.setIdentifier("value");
        AttributeValue val1 = new AttributeValue(value);
        AttributeValue val2 = new AttributeValue("");
        val2.setType("external_identifier");
        val2.setValue("value");
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void ontology_term_value_test() {
        OntologyTerm value = new OntologyTerm();
        value.setTerm_id("id");
        AttributeValue val1 = new AttributeValue(value);
        AttributeValue val2 = new AttributeValue("");
        val2.setType("ontology_term");
        val2.setValue("id");
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void experiment_value_test() {
        Experiment value = new Experiment();
        value.setId("id");
        AttributeValue val1 = new AttributeValue(value);
        AttributeValue val2 = new AttributeValue("");
        val2.setType("experiment");
        val2.setValue("id");
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void analysis_value_test() {
        Analysis value = new Analysis();
        value.setId("id");
        AttributeValue val1 = new AttributeValue(value);
        AttributeValue val2 = new AttributeValue("");
        val2.setType("analysis");
        val2.setValue("id");
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void null_value_test() {
        Object value = null;
        AttributeValue val1 = new AttributeValue(value);
        AttributeValue val2 = new AttributeValue(null);
        val2.setType("null_value");
        val2.setValue(null);
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void attributes_value_test() {
        Attributes value = new Attributes();
        AttributeValue val1 = new AttributeValue(value);
        AttributeValue val2 = new AttributeValue("");
        val2.setType("attributes");
        val2.setValue(value);
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void not_supported_value_test() {
        LinkedList value = new LinkedList();
        try {
            AttributeValue val1 = new AttributeValue(value);
        } catch (TypeNotPresentException e) {
            assertEquals(e.getMessage(), "Type is not supported");
        }

    }

}