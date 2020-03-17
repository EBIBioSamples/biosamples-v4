package uk.ac.ebi.biosamples.livelist;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

@Component
public class LiveListRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveListRunner.class);
    private final BioSamplesClient bioSamplesClient;
    private final PipelinesProperties pipelinesProperties;
    public static final String INSDC_STATUS = "INSDC status";
    public static final String PUBLIC = "public";
    public static final String LIVE = "live";

    @Autowired
    private LiveListPipelineDao liveListPipelineDao;

    public LiveListRunner(final BioSamplesClient bioSamplesClient, final PipelinesProperties pipelinesProperties) {
        this.pipelinesProperties = pipelinesProperties;

        if (bioSamplesClient.getPublicClient().isPresent()) {
            this.bioSamplesClient = bioSamplesClient.getPublicClient().get();
        } else {
            this.bioSamplesClient = bioSamplesClient;
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        doLiveListExport(args);
        doSuppListExport(args);
    }

    private void doSuppListExport(ApplicationArguments args) {
        String suppListFileName = "supplist.txt";

        if (args.getNonOptionArgs().size() > 0) {
            suppListFileName = args.getNonOptionArgs().get(0);
        }

        long startTime = System.nanoTime();
        Writer suppListWriter = null;

        try {
            suppListWriter = args.getOptionValues("gzip") == null
                    ? new OutputStreamWriter(new FileOutputStream(suppListFileName), StandardCharsets.UTF_8)
                    : new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(suppListFileName)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failure to setup supp list writer", e);
            MailSender.sendEmail("Live list pipeline - supplist generation", null, false);
            System.exit(0);
        }

        doSuppListExport(startTime, suppListWriter);
    }

    private void doSuppListExport(final long startTime, final Writer suppListWriter) {
        final AtomicInteger sampleCount = new AtomicInteger();
        AtomicBoolean isPassed = new AtomicBoolean(true);

        liveListPipelineDao.doGetSuppressedEnaSamples().forEach(sample -> {
            try {
                sampleCount.getAndIncrement();
                suppListWriter.write(sample);
                suppListWriter.write("\n");
                suppListWriter.flush();
            } catch (final Exception e) {
                LOGGER.error("Pipeline failed to finish successfully", e);
                isPassed.set(false);
            } finally {
                MailSender.sendEmail("Live list pipeline - livelist generation", null, isPassed.get());
                long elapsed = System.nanoTime() - startTime;
                LOGGER.info("Completed live list export:  " + sampleCount.get() + " samples exported in " + (elapsed / 1000000000L) + "s");
            }
        });
    }

    private void doLiveListExport(ApplicationArguments args) {
        String liveListFileName = "livelist.txt";

        if (args.getNonOptionArgs().size() > 0) {
            liveListFileName = args.getNonOptionArgs().get(0);
        }

        long startTime = System.nanoTime();
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

        doLiveListExport(startTime, liveListWriter);
    }

    private void doLiveListExport(final long startTime, final Writer liveListWriter) {
        LOGGER.info("Starting live list export");
        final Filter statusPublicFilter = FilterBuilder.create().onAttribute(INSDC_STATUS).withValue(PUBLIC).build();
        final Filter statusLiveFilter = FilterBuilder.create().onAttribute(INSDC_STATUS).withValue(LIVE).build();
        final AtomicInteger sampleCount = new AtomicInteger();
        String liveListStatus = "";
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
