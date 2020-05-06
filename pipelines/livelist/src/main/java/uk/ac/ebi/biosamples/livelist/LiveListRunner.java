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
        doKillListExport(args);
    }

    private void doKillListExport(ApplicationArguments args) {
        LOGGER.info("Starting kill list generation");
        Writer killListWriter;
        String killListFileName = "killlist.txt";
        long startTime = System.nanoTime();

        if (args.getNonOptionArgs().size() > 0) {
            killListFileName = args.getNonOptionArgs().get(2);
        }

        try {
            killListWriter = args.getOptionValues("gzipkill") == null
                    ? new OutputStreamWriter(new FileOutputStream(killListFileName), StandardCharsets.UTF_8)
                    : new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(killListFileName)), StandardCharsets.UTF_8);

            doKillListExport(startTime, killListWriter);
        } catch (IOException e) {
            LOGGER.error("Failure to setup killed list writer", e);
            MailSender.sendEmail("Live list pipeline - kill list generation", "Failed to create the writer", false);
        }
    }

    private void doKillListExport(long startTime, Writer killListWriter) {
        final AtomicInteger sampleCount = new AtomicInteger();
        boolean isPassed = true;

        try {
            liveListPipelineDao.doGetKilledEnaSamples().forEach(sampleAccession -> {
                try {
                    sampleCount.getAndIncrement();
                    killListWriter.write(sampleAccession);
                    killListWriter.write("\n");
                    killListWriter.flush();
                } catch (final Exception e) {
                    LOGGER.error("Failed to write + " + sampleAccession);
                }
            });
        } catch (final Exception e) {
            LOGGER.error("Pipeline - livelist, kill list generation failed to finish successfully", e);
            isPassed = false;
        } finally {
            MailSender.sendEmail("Live list pipeline - kill list generation", null, isPassed);
            long elapsed = System.nanoTime() - startTime;
            String logMessage = "Completed kill list export:  " + sampleCount.get() + " samples exported in " + (elapsed / 1000000000L) + "s";
            MailSender.sendEmail("Live list pipeline - kill list generation", logMessage, isPassed);
            LOGGER.info(logMessage);
        }
    }

    private void doSuppListExport(ApplicationArguments args) {
        LOGGER.info("Starting supp list generation");
        Writer suppListWriter;
        String suppListFileName = "supplist.txt";
        long startTime = System.nanoTime();

        if (args.getNonOptionArgs().size() > 0) {
            suppListFileName = args.getNonOptionArgs().get(1);
        }

        try {
            suppListWriter = args.getOptionValues("gzipsupp") == null
                    ? new OutputStreamWriter(new FileOutputStream(suppListFileName), StandardCharsets.UTF_8)
                    : new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(suppListFileName)), StandardCharsets.UTF_8);

            doSuppListExport(startTime, suppListWriter);
        } catch (IOException e) {
            LOGGER.error("Failure to setup supp list writer", e);
            MailSender.sendEmail("Live list pipeline - suppressed list generation", "Failed to create the writer", false);
        }
    }

    private void doSuppListExport(final long startTime, final Writer suppListWriter) {
        final AtomicInteger sampleCount = new AtomicInteger();
        boolean isPassed = true;
        try {
            liveListPipelineDao.doGetSuppressedEnaSamples().forEach(sampleAccession -> {
                try {
                    sampleCount.getAndIncrement();
                    suppListWriter.write(sampleAccession);
                    suppListWriter.write("\n");
                    suppListWriter.flush();
                } catch (final Exception e) {
                    LOGGER.error("Failed to write " + sampleAccession);
                }
            });
        } catch (final Exception e) {
            LOGGER.error("Pipeline - livelist, suppressed list generation failed to finish successfully", e);
            isPassed = false;
        } finally {
            MailSender.sendEmail("Live list pipeline - suppressed list generation", null, isPassed);
            long elapsed = System.nanoTime() - startTime;
            String logMessage = "Completed supp list export:  " + sampleCount.get() + " samples exported in " + (elapsed / 1000000000L) + "s";
            MailSender.sendEmail("Live list pipeline - suppressed list generation", logMessage, isPassed);
            LOGGER.info(logMessage);
        }
    }

    private void doLiveListExport(ApplicationArguments args) {
        LOGGER.info("Starting live list generation");
        Writer liveListWriter;
        String liveListFileName = "livelist.txt";
        long startTime = System.nanoTime();

        if (args.getNonOptionArgs().size() > 0) {
            liveListFileName = args.getNonOptionArgs().get(0);
        }

        try {
            liveListWriter = args.getOptionValues("gziplive") == null
                    ? new OutputStreamWriter(new FileOutputStream(liveListFileName), StandardCharsets.UTF_8)
                    : new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(liveListFileName)), StandardCharsets.UTF_8);

            doLiveListExport(startTime, liveListWriter);
        } catch (IOException e) {
            LOGGER.error("Failure to setup live list writer", e);
            MailSender.sendEmail("Live list pipeline - live list generation", "Failed to create the writer", false);
        }
    }

    private void doLiveListExport(final long startTime, final Writer liveListWriter) {
        LOGGER.info("Starting live list export");
        final Filter statusPublicFilter = FilterBuilder.create().onAttribute(INSDC_STATUS).withValue(PUBLIC).build();
        final Filter statusLiveFilter = FilterBuilder.create().onAttribute(INSDC_STATUS).withValue(LIVE).build();
        final AtomicInteger sampleCount = new AtomicInteger();
        boolean isPassed = true;
        try (liveListWriter; AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true,
                pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

            for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll(Lists.newArrayList(statusPublicFilter, statusLiveFilter))) {
                final Sample sample = sampleResource.getContent();

                executorService.submit(() -> {
                    LOGGER.info("Handling " + sampleResource.getContent().getAccession());

                    if (Instant.now().isAfter(sample.getRelease())) {
                        liveListWriter.write(LiveListUtils.createLiveListString(sample));
                        liveListWriter.write("\n");
                        liveListWriter.flush();
                    }

                    return null;
                });

                sampleCount.getAndIncrement();

                if (sampleCount.get() % 10000 == 0) {
                    LOGGER.info("Running live list export: exported " + sampleCount + " exported samples in " + ((System.nanoTime() - startTime) / 1000000000L) + "s");
                    liveListWriter.close();
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Pipeline failed to finish successfully", e);
            isPassed = false;
        } finally {
            long elapsed = System.nanoTime() - startTime;
            String logMessage = "Completed live list export:  " + sampleCount.get() + " samples exported in " + (elapsed / 1000000000L) + "s";
            MailSender.sendEmail("Live list pipeline - live list generation", logMessage, isPassed);
            LOGGER.info(logMessage);
        }
    }
}
