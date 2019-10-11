package uk.ac.ebi.biosamples.model;

import org.junit.Test;

import java.time.Instant;
import java.util.*;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class SampleTaxIdTest {

    @Test
    public void given_single_ontologyTerm_return_taxId() {
        String olsValue = "http://purl.obolibrary.org/obo/NCBITaxon_10116";
        Attribute attribute = Attribute.build("Organism", "", Collections.singletonList(olsValue), null);
        Sample sample = generateTestSample(Collections.singletonList(attribute));
        assertTrue(10116 == sample.getTaxId());
    }

    @Test
    public void given_single_ontologyTerm_return_taxId_with_lowercase_organism() {
        String olsValue = "http://purl.obolibrary.org/obo/NCBITaxon_9685";
        Attribute attribute = Attribute.build("organism", "Felis catu", Collections.singletonList(olsValue), null);
        Sample sample = generateTestSample(Collections.singletonList(attribute));
        assertTrue(9685 == sample.getTaxId());
    }

    @Test
    public void given_an_Organism_with_multiple_entries() {
        List<String> olsValues = Arrays.asList("http://purl.obolibrary.org/obo/NCBITaxon_10116", "http://purl.obolibrary.org/obo/NCBITaxon_9685");
        Attribute attribute = Attribute.build("Organism", "Felis catu", olsValues, null);
        Sample sample = generateTestSample(Collections.singletonList(attribute));
        assertTrue(10116 == sample.getTaxId());
    }

    @Test
    public void given_multiple_Organisms() {
        String olsValue1 = "http://purl.obolibrary.org/obo/NCBITaxon_10116";
        String olsValue2 = "http://purl.obolibrary.org/obo/NCBITaxon_9685";
        Attribute attribute1 = Attribute.build("Organism", "", olsValue1, null);
        Attribute attribute2 = Attribute.build("Organism", "", olsValue2, null);
        Sample sample = generateTestSample(Arrays.asList(attribute1, attribute2));
        assertTrue(10116 == sample.getTaxId());
    }

    @Test
    public void given_single_ontologyTerm_return_taxId_with_empty_iri() {
        String olsValue = "";
        Attribute attribute = Attribute.build("Organism", "", Collections.singletonList(olsValue), null);
        Sample sample = generateTestSample(Collections.singletonList(attribute));
        assertTrue(0 == sample.getTaxId());
    }

    @Test
    public void given_9606_ontologyTerm_return_taxId() {
        String value = "9606";
        Attribute attribute = Attribute.build("Organism", "", Collections.singletonList(value), null);
        Sample sample = generateTestSample(Collections.singletonList(attribute));
        assertTrue(9606 == sample.getTaxId());
    }

    @Test
    public void given_no_ontologyTerm_return_unknown_taxId() {
        Attribute attribute = Attribute.build("Organism", "s", Collections.EMPTY_LIST, null);
        Sample sample = generateTestSample(Collections.singletonList(attribute));
        assertTrue(0 == sample.getTaxId());
    }

    private Sample generateTestSample(List<Attribute> attributes) {
        Set<Attribute> attributeSet = new HashSet<>();
        for (Attribute attribute : attributes) {
            attributeSet.add(attribute);
        }
        return Sample.build("", "", "", Instant.now(), Instant.now(), Instant.now(), attributeSet, Collections.emptySet(), Collections.emptySet());
    }
}
