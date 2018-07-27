package uk.ac.ebi.biosamples.model.ga4gh_tests;

import org.junit.Test;
import uk.ac.ebi.biosamples.model.ga4gh.*;

import java.util.LinkedList;

import static org.junit.Assert.*;

public class AttributeValueTest {
    @Test
    public void string_value_test() {
        String value = "value";
        Ga4ghAttributeValue val1 = new Ga4ghAttributeValue(value);
        Ga4ghAttributeValue val2 = new Ga4ghAttributeValue("");
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
        Ga4ghAttributeValue val1 = new Ga4ghAttributeValue(value);
        Ga4ghAttributeValue val2 = new Ga4ghAttributeValue(false);
        val2.setType("bool_value");
        val2.setValue(true);
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void double_value_test() {
        Double value = 1.11111;
        Ga4ghAttributeValue val1 = new Ga4ghAttributeValue(value);
        Ga4ghAttributeValue val2 = new Ga4ghAttributeValue(0.111);
        val2.setType("double_value");
        val2.setValue(1.11111);
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void external_identifier_value_test() {
        ExternalIdentifier value = new ExternalIdentifier();
        value.setIdentifier("value");
        Ga4ghAttributeValue val1 = new Ga4ghAttributeValue(value);
        Ga4ghAttributeValue val2 = new Ga4ghAttributeValue("");
        val2.setType("external_identifier");
        val2.setValue("value");
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void ontology_term_value_test() {
        OntologyTerm value = new OntologyTerm();
        value.setTerm_id("id");
        Ga4ghAttributeValue val1 = new Ga4ghAttributeValue(value);
        Ga4ghAttributeValue val2 = new Ga4ghAttributeValue("");
        val2.setType("ontology_term");
        val2.setValue("id");
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void experiment_value_test() {
        Experiment value = new Experiment();
        value.setId("id");
        Ga4ghAttributeValue val1 = new Ga4ghAttributeValue(value);
        Ga4ghAttributeValue val2 = new Ga4ghAttributeValue("");
        val2.setType("experiment");
        val2.setValue("id");
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void analysis_value_test() {
        Analysis value = new Analysis();
        value.setId("id");
        Ga4ghAttributeValue val1 = new Ga4ghAttributeValue(value);
        Ga4ghAttributeValue val2 = new Ga4ghAttributeValue("");
        val2.setType("analysis");
        val2.setValue("id");
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void null_value_test() {
        Object value = null;
        Ga4ghAttributeValue val1 = new Ga4ghAttributeValue(value);
        Ga4ghAttributeValue val2 = new Ga4ghAttributeValue(null);
        val2.setType("null_value");
        val2.setValue(null);
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void attributes_value_test() {
        Ga4ghAttributes value = new Ga4ghAttributes();
        Ga4ghAttributeValue val1 = new Ga4ghAttributeValue(value);
        Ga4ghAttributeValue val2 = new Ga4ghAttributeValue("");
        val2.setType("attributes");
        val2.setValue(value);
        assertEquals(val1.getType(), val2.getType());
    }

    @Test
    public void not_supported_value_test() {
        LinkedList value = new LinkedList();
        try {
            Ga4ghAttributeValue val1 = new Ga4ghAttributeValue(value);
        } catch (TypeNotPresentException e) {
            assertEquals(e.getMessage(), "Type is not supported");
        }

    }

}