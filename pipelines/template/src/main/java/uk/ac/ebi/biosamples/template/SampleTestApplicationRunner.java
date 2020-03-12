package uk.ac.ebi.biosamples.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

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
        getAllSamples();
    }

    private void getAllSamples() {
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

    private Set<String> getExistingPublicNcbiAccessions() {
        log.info("getting existing public ncbi accessions");
        long startTime = System.nanoTime();
        //make sure to only get the public samples
        Set<String> existingAccessions = new TreeSet<>();
        Iterable<Resource<Sample>> samples = bioSamplesClient.getPublicClient().get().fetchSampleResourceAll(Collections.singleton(FilterBuilder.create().onAccession("SAM[^E].*").build()));
        for (Resource<Sample> sample : samples) {
            existingAccessions.add(sample.getContent().getAccession());
        }
        long endTime = System.nanoTime();
        double intervalSec = ((double) (endTime - startTime)) / 1000000000.0;
        log.debug("Took " + intervalSec + "s to get " + existingAccessions.size() + " existing public ncbi accessions");
        return existingAccessions;
    }

    private void getSAMD00000001() {
        log.info("get SAMD00000001");
        long startTime = System.nanoTime();
        Optional<Resource<Sample>> sample = bioSamplesClient.getPublicClient().get().fetchSampleResource("SAMD00000001");
        long endTime = System.nanoTime();
        double intervalSec = ((double) (endTime - startTime)) / 1000000000.0;
        log.info("Took " + intervalSec + "s to get SAMD00000001");
    }
}
