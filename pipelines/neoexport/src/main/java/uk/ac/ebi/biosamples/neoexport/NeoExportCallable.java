package uk.ac.ebi.biosamples.neoexport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo4j.model.NeoSample;
import uk.ac.ebi.biosamples.neo4j.repo.NeoSampleRepository;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NeoExportCallable implements Callable<PipelineResult> {
    private static final Logger LOG = LoggerFactory.getLogger(NeoExportCallable.class);
    static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

    private final Sample sample;
    private final NeoSampleRepository neoSampleRepository;

    public NeoExportCallable(NeoSampleRepository neoSampleRepository, Sample sample) {
        this.neoSampleRepository = neoSampleRepository;
        this.sample = sample;
    }

    @Override
    public PipelineResult call() {
        try {
            NeoSample neoSample = NeoSample.build(sample);
            neoSampleRepository.loadSample(neoSample);
        } catch (Exception e) {
            failedQueue.add(sample.getAccession());
            LOG.error("Failed to load sample: " + sample.getAccession(), e);
        }
        return new PipelineResult(sample.getAccession(), 0, true);
    }
}
