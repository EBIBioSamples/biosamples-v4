package uk.ac.ebi.biosamples.clearinghouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.ClearinghouseCurations;
import uk.ac.ebi.biosamples.model.ClearinghouseSampleData;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.MailSender;

import java.util.Optional;

@Component
public class ClearinghouseRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClearinghouseRunner.class);
    private static final String CLEARINGHOUSE_API_ENDPOINT = "https://www.ebi.ac.uk/ena/clearinghouse/api/curations/";
    private final BioSamplesClient bioSamplesClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PipelinesProperties pipelinesProperties;

    public ClearinghouseRunner(final BioSamplesClient bioSamplesClient) {
        if (bioSamplesClient.getPublicClient().isPresent()) {
            this.bioSamplesClient = bioSamplesClient.getPublicClient().get();
        } else {
            this.bioSamplesClient = bioSamplesClient;
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        doGetClearingHouseCurations(args);
    }

    private void doGetClearingHouseCurations(ApplicationArguments args) {
        boolean isPassed = true;

        long startTime = System.nanoTime();
        int sampleCount = 0;

        try (final AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true,
                pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {
            LOGGER.info("Starting clearinghouse pipeline");

            //for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll()) {
            Optional<Resource<Sample>> sampleResource = bioSamplesClient.fetchSampleResource("SAMEA102066418");
            executorService.submit(() -> {
                LOGGER.trace("Handling " + sampleResource);
                try {
                    Sample sample = sampleResource.get().getContent();

                    ResponseEntity<ClearinghouseSampleData> response
                            = restTemplate.getForEntity(CLEARINGHOUSE_API_ENDPOINT + sample.getAccession(), ClearinghouseSampleData.class);

                    for(ClearinghouseCurations curation : response.getBody().getCurations()) {
                        System.out.println(curation.getAttributePre());
                    }

                    LOGGER.info("Response status " + response.getStatusCode());
                    LOGGER.info("Response String " + response.getBody());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
            //}
        } catch (final Exception e) {
            e.printStackTrace();
            LOGGER.error("Clearinghouse pipeline failed to finish successfully", e);
            isPassed = false;
        } finally {
            long elapsed = System.nanoTime() - startTime;
            String logMessage = "Completed Clearinghouse pipeline:  " + sampleCount + " samples curated in " + (elapsed / 1000000000L) + "s";
            LOGGER.info(logMessage);
            MailSender.sendEmail("Clearinghouse pipeline", logMessage, isPassed);
        }
    }
}
