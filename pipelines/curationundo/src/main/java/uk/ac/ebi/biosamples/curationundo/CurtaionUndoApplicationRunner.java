package uk.ac.ebi.biosamples.curationundo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Component
public class CurtaionUndoApplicationRunner implements ApplicationRunner {

    private Logger log = LoggerFactory.getLogger(getClass());

    private final BioSamplesClient bioSamplesClient;

    public CurtaionUndoApplicationRunner(BioSamplesClient bioSamplesClient) {
        this.bioSamplesClient = bioSamplesClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 2)) {
            Map<String, Future<Void>> futures = new HashMap<>();
            long samplesQueued = 0;
            long startTime = System.currentTimeMillis();
            try {
                for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll("", Collections.emptyList())) {
                    String accession = sampleResource.getContent().getAccession();
                    samplesQueued++;
                    boolean canary = (samplesQueued % 1000 == 0);
                    Callable<Void> task = new CurationUndoCallable(bioSamplesClient, accession, canary);
                    futures.put(accession, executorService.submit(task));
                    if (canary) {
                        long endTime = System.currentTimeMillis();
                        long duration = (endTime - startTime);
                        log.info("PROCESSED: samples:" + samplesQueued + " rate: " + samplesQueued / ((duration / 1000) + 1) + " samples per second");
                    }
                }
            } catch (IllegalStateException e) {
                log.error("Error", e);
            }
        }
    }
}
