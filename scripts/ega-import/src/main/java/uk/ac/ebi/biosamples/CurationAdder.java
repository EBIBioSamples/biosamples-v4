package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;

public class CurationAdder {
    private static final Logger LOG = LoggerFactory.getLogger(EGAImportRunner.class);

    private final BioSamplesClient client;

    public CurationAdder(BioSamplesClient client) {
        this.client = client;
    }

    public void addCuration(String accession, Attribute preAttribute, Attribute postAttribute) {
        LOG.info("Adding curation");
    }

    public void deleteCuration(String accession, Attribute attribute) {
        LOG.info("Adding curation");
    }


}
