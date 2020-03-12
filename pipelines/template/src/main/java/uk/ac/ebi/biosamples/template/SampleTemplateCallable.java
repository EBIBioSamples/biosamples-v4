package uk.ac.ebi.biosamples.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SampleTemplateCallable implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(SampleTemplateCallable.class);
    static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

    private final Sample sample;
    private final BioSamplesClient bioSamplesClient;
    private final String domain;
    private final Map<String, String> curationRules;

    public SampleTemplateCallable(BioSamplesClient bioSamplesClient, Sample sample, String domain,
                                  Map<String, String> curationRules) {
        this.bioSamplesClient = bioSamplesClient;
        this.sample = sample;
        this.domain = domain;
        this.curationRules = curationRules;
    }

    @Override
    public Integer call() {
        int appliedCurations = 0;
        try {
            appliedCurations = doSomeWorkHere(sample);
        } catch (Exception e) {
            failedQueue.add(sample.getAccession());
            LOG.error("Failed to add curation on sample: " + sample.getAccession(), e);
        }
        return appliedCurations;
    }

    private int doSomeWorkHere(Sample sample) {
        LOG.info("Sample accession: {}", sample.getAccession());
        int curations = 10;
        return curations;
    }
}
