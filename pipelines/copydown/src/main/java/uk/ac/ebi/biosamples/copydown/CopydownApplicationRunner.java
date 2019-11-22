package uk.ac.ebi.biosamples.copydown;

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
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ArgUtils;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Component
public class CopydownApplicationRunner implements ApplicationRunner {
    private final BioSamplesClient bioSamplesClient;
    private final PipelinesProperties pipelinesProperties;
    private Logger log = LoggerFactory.getLogger(getClass());

    public CopydownApplicationRunner(final BioSamplesClient bioSamplesClient,
                                     final PipelinesProperties pipelinesProperties) {
        this.bioSamplesClient = bioSamplesClient;
        this.pipelinesProperties = pipelinesProperties;
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        final Collection<Filter> filters = ArgUtils.getDateFilters(args);
        boolean isPassed = true;

        try (final AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true,
                pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {
            final Map<String, Future<Void>> futures = new HashMap<>();

            for (final Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll("", filters)) {
                log.trace("Handling " + sampleResource);
                final Sample sample = sampleResource.getContent();

                if (sample == null) {
                    throw new RuntimeException("Sample should not be null");
                }

                final Callable<Void> task = new SampleCopydownCallable(bioSamplesClient, sample,
                        pipelinesProperties.getCopydownDomain());

                futures.put(sample.getAccession(), executorService.submit(task));
            }

            log.info("waiting for futures");
            // wait for anything to finish
            ThreadUtils.checkFutures(futures, 0);
        } catch (Exception e) {
            log.error("Pipeline failed to finish successfully", e);
            isPassed = false;
            throw e;
        } finally {
            //now print a list of things that failed
            if (SampleCopydownCallable.failedQueue.size() > 0) {
                //put the first ones on the queue into a list
                //limit the size of list to avoid overload
                final List<String> fails = new LinkedList<>();

                while (fails.size() < 100 && SampleCopydownCallable.failedQueue.peek() != null) {
                    fails.add(SampleCopydownCallable.failedQueue.poll());
                }

                final String failures = "Failed files (" + fails.size() + ") " + String.join(" , ", fails);

                log.info(failures);
                MailSender.sendEmail("Copy-down", failures, isPassed);
            }
        }
        //TODO re-check existing curations
    }
}
