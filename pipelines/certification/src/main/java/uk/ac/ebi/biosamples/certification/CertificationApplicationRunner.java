package uk.ac.ebi.biosamples.certification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Component
@Profile("!test")
public class CertificationApplicationRunner implements ApplicationRunner {

    private Logger log = LoggerFactory.getLogger(getClass());

    private final BioSamplesClient bioSamplesClient;
    private final RestTemplate restTemplate;
    private final PipelinesProperties pipelinesProperties;

    public CertificationApplicationRunner(BioSamplesClient bioSamplesClient, RestTemplate restTemplate, PipelinesProperties pipelinesProperties) {
        this.bioSamplesClient = bioSamplesClient;
        this.restTemplate = restTemplate;
        this.pipelinesProperties = pipelinesProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 2)) {
            Map<String, Future<Void>> futures = new HashMap<>();
            long samplesQueued = 0;
            long startTime = System.currentTimeMillis();
            long limit = 100_000_000L;
            try {
                for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll("", Collections.emptyList())) {
                        Sample sample = sampleResource.getContent();
                        samplesQueued++;
                        boolean canary = (samplesQueued % 1000 == 0);
                        Callable<Void> task = new CertificationCallable(bioSamplesClient, restTemplate, sample, pipelinesProperties);
                        futures.put(sample.getAccession(), executorService.submit(task));
                        if (canary) {
                            long endTime = System.currentTimeMillis();
                            long duration = (endTime - startTime);
                            log.info("PROCESSED: samples:" + samplesQueued + " rate: " + samplesQueued / ((duration / 1000) + 1) + " samples per second");
                        }
                        if (samplesQueued >= limit) {
                            break;
                        }
                }
            } catch (IllegalStateException e) {
                log.error("Error", e);
            }
        }
    }
}
