package uk.ac.ebi.biosamples.template;

import uk.ac.ebi.biosamples.PipelineSampleCallable;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleTemplateCallable extends PipelineSampleCallable {
    private final String domain;

    public SampleTemplateCallable(BioSamplesClient bioSamplesClient, String domain) {
        super(bioSamplesClient);
        this.domain = domain;
    }

    @Override
    public int processSample(Sample sample) throws Exception {
        return 1;
    }
}
