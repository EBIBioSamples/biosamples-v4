package uk.ac.ebi.biosamples.livelist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.zip.GZIPOutputStream;

@Component
public class LiveListRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveListRunner.class);

    private final BioSamplesClient bioSamplesClient;

    public LiveListRunner(BioSamplesClient bioSamplesClient) {
        //ensure the client is public
        if (bioSamplesClient.getPublicClient().isPresent()) {
            this.bioSamplesClient = bioSamplesClient.getPublicClient().get();
        } else {
            this.bioSamplesClient = bioSamplesClient;
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String liveListFilename = "livelist.txt";
        if (args.getNonOptionArgs().size() > 0) {
            liveListFilename = args.getNonOptionArgs().get(0);
        }
        long startTime = System.nanoTime();
        int sampleCount = 0;
        try {
            try (
                    Writer liveListWriter = args.getOptionValues("gzip") == null
                            ? new OutputStreamWriter(new FileOutputStream(liveListFilename), "UTF-8")
                            : new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(liveListFilename)), "UTF-8");
            ) {
                LOGGER.info("Starting live list export");
                for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll()) {
                    LOGGER.trace("Handling " + sampleResource);
                    Sample sample = sampleResource.getContent();
                    if (Instant.now().isAfter(sample.getRelease())) {
                        liveListWriter.write(LiveListUtils.createLiveListString(sample));
                        liveListWriter.write("\n");
                        sampleCount++;
                    }
                    if (sampleCount % 10000 == 0) {
                        LOGGER.info("Running live list export: exported " + sampleCount + " exported samples in " + ((System.nanoTime() - startTime) / 1000000000l) + "s");
                    }
                }
            } finally {
            }
        } finally {
        }
        long elapsed = System.nanoTime() - startTime;
        LOGGER.info("Completed live list export:  " + sampleCount + " samples exported in " + (elapsed / 1000000000l) + "s");
    }

}
