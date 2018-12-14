package uk.ac.ebi.biosamples.model;

import org.junit.Ignore;
import org.junit.Test;
import uk.ac.ebi.biosamples.service.OLSDataRetriever;

import static org.junit.Assert.assertEquals;

public class OLSDataRetrieverTest {
    @Test
    public void id_retrieving_test() {
        OLSDataRetriever retriever = new OLSDataRetriever();
        retriever.readOntologyJsonFromUrl("http://purl.obolibrary.org/obo/NCBITaxon_9606");
        String expected_id = "NCBITaxon:9606";
        String actual_id = retriever.getOntologyTermId();
        assertEquals(actual_id, expected_id);
    }

    @Test
    public void label_retreiving_test() {
        OLSDataRetriever retriever = new OLSDataRetriever();
        retriever.readOntologyJsonFromUrl("http://purl.obolibrary.org/obo/NCBITaxon_9606");
        String expected_label = "Homo sapiens";
        String actual_label = retriever.getOntologyTermLabel();
        assertEquals(actual_label, expected_label);
    }

}