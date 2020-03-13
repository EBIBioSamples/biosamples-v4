package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.concurrent.Callable;

public abstract class PipelineSampleCallable implements Callable<PipelineResult> {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    protected Sample sample;
    private final BioSamplesClient bioSamplesClient;

    public PipelineSampleCallable(BioSamplesClient bioSamplesClient) {
        this.bioSamplesClient = bioSamplesClient;
    }

    @Override
    public PipelineResult call() {
        boolean success = true;
        int appliedCurations = 0;
        try {
            appliedCurations = processSample(sample);
        } catch (Exception e) {
            success = false;
            LOG.error("Failed to add curation on sample: " + sample.getAccession(), e);
        }
        return new PipelineResult(sample.getAccession(), appliedCurations, success);
    }

    public PipelineSampleCallable withSample(Sample sample) {
        this.sample = sample;
        return this;
    }

    public abstract int processSample(Sample sample) throws Exception;
}
