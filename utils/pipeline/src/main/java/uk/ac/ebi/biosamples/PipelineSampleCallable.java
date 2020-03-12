package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.concurrent.Callable;

public abstract class PipelineSampleCallable implements Callable<PipelineResult> {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final Sample sample;
    private final BioSamplesClient bioSamplesClient;
    private final String domain;

    public PipelineSampleCallable(BioSamplesClient bioSamplesClient, Sample sample, String domain) {
        this.bioSamplesClient = bioSamplesClient;
        this.sample = sample;
        this.domain = domain;
    }

    @Override
    public PipelineResult call() {
        boolean success = true;
        int appliedCurations = 0;
        try {
            appliedCurations = doSomeWorkHere(sample);
        } catch (Exception e) {
            success = false;
            LOG.error("Failed to add curation on sample: " + sample.getAccession(), e);
        }
        return new PipelineResult(sample.getAccession(), appliedCurations, success);
    }

    public abstract int doSomeWorkHere(Sample sample);
}
