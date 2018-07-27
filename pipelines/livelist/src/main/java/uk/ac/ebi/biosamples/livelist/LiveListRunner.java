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
        String liveListFilename = args.getNonOptionArgs().get(0);
        long startTime = System.nanoTime();
        int sampleCount = 0;
        try {
            try (
                    Writer liveListWriter = args.getOptionValues("gzip") == null
                            ? new OutputStreamWriter(new FileOutputStream(liveListFilename), "UTF-8")
                            : new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(liveListFilename)), "UTF-8");
            ) {
                for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll()) {
                    LOGGER.trace("Handling " + sampleResource);
                    Sample sample = sampleResource.getContent();
                    liveListWriter.write(LiveListUtils.createLiveListString(sample));
                    liveListWriter.write("\n");
                    sampleCount++;
                }
            } finally {
            }
        } finally {
        }
        long elapsed = System.nanoTime() - startTime;
        LOGGER.info("Live list exported " + sampleCount + " samples in " + (elapsed / 1000000000l) + "s");
    }

}
