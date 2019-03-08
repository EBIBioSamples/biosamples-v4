package uk.ac.ebi.biosamples.sampletest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collections;

@Component
public class SampleTestApplicationRunner implements ApplicationRunner {

    private Logger log = LoggerFactory.getLogger(getClass());

    private final BioSamplesClient bioSamplesClient;

    @Autowired
    public SampleTestApplicationRunner(BioSamplesClient bioSamplesClient) {
        this.bioSamplesClient = bioSamplesClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("not-founds.txt"))) {
            long count = 0;
            long startTime = System.currentTimeMillis();

            for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll("", Collections.emptyList())) {
                try {
                    String accession = sampleResource.getContent().getAccession();
                    log.debug(String.format("got %s", accession));
                    count++;
                    boolean canary = (count % 1000 == 0);
                    {
                        if (canary) {
                            long endTime = System.currentTimeMillis();
                            long duration = (endTime - startTime);
                            log.info("PROCESSED: samples:" + count + " rate: " + count / ((duration / 1000) + 1) + " samples per second");
                        }
                    }
                } catch (Exception e) {
                    log.error("failed after: " + count);
                    log.error("Error getting individual sample resource: " + sampleResource.toString(), e);
                    writer.write(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error getting samples resources", e);
        }
    }
}
