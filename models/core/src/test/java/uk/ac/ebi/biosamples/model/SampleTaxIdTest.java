package uk.ac.ebi.biosamples.model;

import org.junit.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class SampleTaxIdTest {

    @Test
    public void given_single_ontologyTerm_return_taxId() {
        String olsValue = "http://purl.obolibrary.org/obo/NCBITaxon_10116";
        Integer[] taxIds = new Integer[]{10116};
        Attribute attribute = Attribute.build("Organism", "", Collections.singletonList(olsValue), null);
        Sample sample = generateTestSample(attribute);
        assertEquals(taxIds, sample.getTaxId());
    }

    @Test
    public void given_single_ontologyTerm_return_taxId_with_lowercase_organism() {
        String olsValue = "http://purl.obolibrary.org/obo/NCBITaxon_9685";
        Integer[] taxIds = new Integer[]{9685};
        Attribute attribute = Attribute.build("organism", "Felis catu", Collections.singletonList(olsValue), null);
        Sample sample = generateTestSample(attribute);
        assertEquals(taxIds, sample.getTaxId());
    }

    @Test
    public void given_single_ontologyTerm_return_taxId_with_empty_iri() {
        String olsValue = "";
        Attribute attribute = Attribute.build("Organism", "", Collections.singletonList(olsValue), null);
        Sample sample = generateTestSample(attribute);
        assertEquals(new Integer[]{0}, sample.getTaxId());
    }

    @Test
    public void given_9606_ontologyTerm_return_taxId() {
        String value = "9606";
        Attribute attribute = Attribute.build("Organism", "", Collections.singletonList(value), null);
        Sample sample = generateTestSample(attribute);
        assertEquals(new Integer[]{Integer.parseInt(value)}, sample.getTaxId());
    }

    @Test
    public void given_no_ontologyTerm_return_unknown_taxId() {
        Attribute attribute = Attribute.build("Organism", "s", Collections.EMPTY_LIST, null);
        Sample sample = generateTestSample(attribute);
        assertEquals(new Integer[0], sample.getTaxId());
    }

    private Sample generateTestSample(Attribute attribute) {
        Set<Attribute> attributeSet = new HashSet<>();
        attributeSet.add(attribute);
        return Sample.build("", "", "", Instant.now(), Instant.now(), attributeSet, Collections.emptySet(), Collections.emptySet());
    }
}
