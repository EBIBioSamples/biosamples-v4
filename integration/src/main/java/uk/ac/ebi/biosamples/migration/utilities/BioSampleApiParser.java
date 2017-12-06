package uk.ac.ebi.biosamples.migration.utilities;

import uk.ac.ebi.biosamples.model.Sample;

public interface BioSampleApiParser {
    public Sample getSample(String accession);
}
