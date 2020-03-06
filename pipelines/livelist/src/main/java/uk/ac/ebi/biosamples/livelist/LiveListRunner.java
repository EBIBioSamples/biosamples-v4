package uk.ac.ebi.biosamples.livelist;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.MailSender;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

@Component
public class LiveListRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveListRunner.class);
    public static final String INSDC_STATUS = "INSDC status";
    public static final String PUBLIC = "public";
    public static final String LIVE = "live";
    private final BioSamplesClient bioSamplesClient;
    private final PipelinesProperties pipelinesProperties;

    public LiveListRunner(final BioSamplesClient bioSamplesClient, PipelinesProperties pipelinesProperties) {
        this.pipelinesProperties = pipelinesProperties;

        if (bioSamplesClient.getPublicClient().isPresent()) {
            this.bioSamplesClient = bioSamplesClient.getPublicClient().get();
        } else {
            this.bioSamplesClient = bioSamplesClient;
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        generateLiveListImport(args);
    }

    private void generateLiveListImport(ApplicationArguments args) {
        String liveListFileName = "C:\\Users\\dgupta\\livelist.txt";
        String liveListStatus = "";

        if (args.getNonOptionArgs().size() > 0) {
            liveListFileName = args.getNonOptionArgs().get(0);
        }

        long startTime = System.nanoTime();
        AtomicInteger sampleCount = new AtomicInteger();

        Writer liveListWriter = null;

        try {
            liveListWriter = args.getOptionValues("gzip") == null
                    ? new OutputStreamWriter(new FileOutputStream(liveListFileName), StandardCharsets.UTF_8)
                    : new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(liveListFileName)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failure to setup live list writer", e);
            MailSender.sendEmail("Live list pipeline - livelist generation", null, false);
            System.exit(0);
        }

        doLiveListExport(liveListStatus, startTime, sampleCount, liveListWriter);
    }

    private void doLiveListExport(String liveListStatus, long startTime, AtomicInteger sampleCount, Writer liveListWriter) {
        LOGGER.info("Starting live list export");
        final Filter statusPublicFilter = FilterBuilder.create().onAttribute(INSDC_STATUS).withValue(PUBLIC).build();
        final Filter statusLiveFilter = FilterBuilder.create().onAttribute(INSDC_STATUS).withValue(LIVE).build();
        boolean isPassed = true;

        try (liveListWriter; AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true,
                pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

            for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll(Lists.newArrayList(statusPublicFilter, statusLiveFilter))) {
                final Sample sample = sampleResource.getContent();
                final Writer finalLiveListWriter = liveListWriter;

                executorService.submit(() -> {
                    LOGGER.info("Handling " + sampleResource.getContent().getAccession());

                    if (Instant.now().isAfter(sample.getRelease())) {
                        finalLiveListWriter.write(LiveListUtils.createLiveListString(sample));
                        finalLiveListWriter.write("\n");
                        finalLiveListWriter.flush();
                    }

                    return null;
                });

                sampleCount.getAndIncrement();

                if (sampleCount.get() % 10000 == 0) {
                    liveListStatus = "Running live list export: exported " + sampleCount + " exported samples in " + ((System.nanoTime() - startTime) / 1000000000L) + "s";
                    LOGGER.info(liveListStatus);
                    finalLiveListWriter.close();
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Pipeline failed to finish successfully", e);
            isPassed = false;
        } finally {
            MailSender.sendEmail("Live list pipeline - livelist generation", liveListStatus, isPassed);
            long elapsed = System.nanoTime() - startTime;
            LOGGER.info("Completed live list export:  " + sampleCount.get() + " samples exported in " + (elapsed / 1000000000L) + "s");
        }
    }
}
