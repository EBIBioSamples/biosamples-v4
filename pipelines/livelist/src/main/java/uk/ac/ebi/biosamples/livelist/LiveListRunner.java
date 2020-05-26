package uk.ac.ebi.biosamples.livelist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.MailSender;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

@Component
public class LiveListRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveListRunner.class);
    private final BioSamplesClient bioSamplesClient;
    private static final String SUPPRESSED = "suppressed";
    private static final String KILLED = "killed";

    @Autowired
    private LiveListPipelineDao liveListPipelineDao;

    public LiveListRunner(final BioSamplesClient bioSamplesClient) {
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

    private void doLiveListExport(ApplicationArguments args) {
        String liveListFilename = "livelist.txt";
        boolean isPassed = true;

        if (args.getNonOptionArgs().size() > 0) {
            liveListFilename = args.getNonOptionArgs().get(0);
        }

        long startTime = System.nanoTime();
        int sampleCount = 0;

        try {
            try (
                    Writer liveListWriter = args.getOptionValues("gziplive") == null
                            ? new OutputStreamWriter(new FileOutputStream(liveListFilename), "UTF-8")
                            : new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(liveListFilename)), "UTF-8");
            ) {
                LOGGER.info("Starting live list export");

                for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll()) {
                    Sample sample = sampleResource.getContent();

                    if (Instant.now().isAfter(sample.getRelease())) {
                        liveListWriter.write(LiveListUtils.createLiveListString(sample));
                        liveListWriter.write("\n");
                        liveListWriter.flush();
                        sampleCount++;
                    }

                    if (sampleCount % 10000 == 0) {
                        LOGGER.info("Running live list export: exported " + sampleCount + " exported samples in " + ((System.nanoTime() - startTime) / 1000000000l) + "s");
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Live list pipeline - live list generation failed to finish successfully", e);
            isPassed = false;
        } finally {
            long elapsed = System.nanoTime() - startTime;
            String logMessage = "Completed live list export:  " + sampleCount + " samples exported in " + (elapsed / 1000000000L) + "s";
            LOGGER.info(logMessage);
            MailSender.sendEmail("Live list pipeline - live list generation", logMessage, isPassed);
        }
    }

    private void doSuppListExport(ApplicationArguments args) {
        LOGGER.info("Starting supp list generation");
        String suppListFileName = "supplist.txt";
        long startTime = System.nanoTime();

        if (args.getNonOptionArgs().size() > 0) {
            suppListFileName = args.getNonOptionArgs().get(1);
        }

        try {
            try (Writer suppListWriter = args.getOptionValues("gzipsupp") == null
                    ? new OutputStreamWriter(new FileOutputStream(suppListFileName), StandardCharsets.UTF_8)
                    : new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(suppListFileName)), StandardCharsets.UTF_8)) {
                doSuppListExport(startTime, suppListWriter);
            }
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
                    final Optional<Resource<Sample>> optionalSampleResource = bioSamplesClient.fetchSampleResource(sampleAccession,
                            Optional.of(new ArrayList<>()));
                    AtomicBoolean qualifyForSuppressedList = new AtomicBoolean(false);

                    if (optionalSampleResource.isPresent()) {
                        final Sample sample = optionalSampleResource.get().getContent();

                        sample.getAttributes().forEach(attribute -> {
                            if (attribute.getType().equals("INSDC status") && attribute.getValue().equals(SUPPRESSED)) {
                                qualifyForSuppressedList.set(true);
                            }
                        });

                        if (qualifyForSuppressedList.get()) {
                            LOGGER.info("Writing " + sampleAccession);
                            sampleCount.getAndIncrement();
                            suppListWriter.write(sampleAccession);
                            suppListWriter.write("\n");
                            suppListWriter.flush();
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    LOGGER.error("Failed to write " + sampleAccession);
                }
            });
        } catch (final Exception e) {
            LOGGER.error("Live list pipeline - suppressed list generation failed to finish successfully", e);
            isPassed = false;
        } finally {
            long elapsed = System.nanoTime() - startTime;
            String logMessage = "Completed supp list export:  " + sampleCount.get() + " samples exported in " + (elapsed / 1000000000L) + "s";
            LOGGER.info(logMessage);
            MailSender.sendEmail("Live list pipeline - suppressed list generation", logMessage, isPassed);
        }
    }

    private void doKillListExport(ApplicationArguments args) {
        LOGGER.info("Starting kill list generation");
        String killListFileName = "killlist.txt";
        long startTime = System.nanoTime();

        if (args.getNonOptionArgs().size() > 0) {
            killListFileName = args.getNonOptionArgs().get(2);
        }

        try {
            try (Writer killListWriter = args.getOptionValues("gzipkill") == null
                    ? new OutputStreamWriter(new FileOutputStream(killListFileName), StandardCharsets.UTF_8)
                    : new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(killListFileName)), StandardCharsets.UTF_8)) {

                doKillListExport(startTime, killListWriter);
            }
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
                LOGGER.info("Handling " + sampleAccession);
                try {
                    final Optional<Resource<Sample>> optionalSampleResource = bioSamplesClient.fetchSampleResource(sampleAccession,
                            Optional.of(new ArrayList<>()));

                    if (!optionalSampleResource.isPresent()) {
                        LOGGER.info("Writing " + sampleAccession);
                        sampleCount.getAndIncrement();
                        killListWriter.write(sampleAccession);
                        killListWriter.write("\n");
                        killListWriter.flush();
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    LOGGER.error("Failed to write " + sampleAccession);
                }
            });
        } catch (final Exception e) {
            LOGGER.error("Live list pipeline - kill list generation failed to finish successfully", e);
            isPassed = false;
        } finally {
            long elapsed = System.nanoTime() - startTime;
            String logMessage = "Completed kill list export:  " + sampleCount.get() + " samples exported in " + (elapsed / 1000000000L) + "s";
            LOGGER.info(logMessage);
            MailSender.sendEmail("Live list pipeline - kill list generation", logMessage, isPassed);
        }
    }
}
