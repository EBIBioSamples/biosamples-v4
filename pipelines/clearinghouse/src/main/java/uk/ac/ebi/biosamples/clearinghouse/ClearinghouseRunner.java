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
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.MailSender;

import java.util.Optional;

@Component
public class ClearinghouseRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClearinghouseRunner.class);
    private static final String CLEARINGHOUSE_API_ENDPOINT = "https://www.ebi.ac.uk/ena/clearinghouse/api/curations/";
    private final BioSamplesClient bioSamplesClient;

    @Autowired
    RestTemplate restTemplate;

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

        try {
            LOGGER.info("Starting clearinghouse pipeline");

            // We will replace this with call to fetch all samples
            Optional<Resource<Sample>> sampleResource = bioSamplesClient.fetchSampleResource("SAMEA102066418");
                Sample sample = sampleResource.get().getContent();

                ResponseEntity<String> response
                        = restTemplate.getForEntity(CLEARINGHOUSE_API_ENDPOINT + sample.getAccession(), String.class);

                LOGGER.info("Response status " + response.getStatusCode());
                LOGGER.info("Response String " + response.getBody());

        } catch (final Exception e) {
            LOGGER.error("Live list pipeline - live list generation failed to finish successfully", e);
            isPassed = false;
        } finally {
            long elapsed = System.nanoTime() - startTime;
            String logMessage = "Completed Clearinghouse pipeline:  " + sampleCount + " samples curated in " + (elapsed / 1000000000L) + "s";
            LOGGER.info(logMessage);
            MailSender.sendEmail("Clearinghouse pipeline", logMessage, isPassed);
        }
    }
}
