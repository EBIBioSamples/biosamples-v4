package uk.ac.ebi.biosamples.deduplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.MailSender;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Component
public class DeduplicationRunner implements ApplicationRunner {
    private Logger log = LoggerFactory.getLogger(DeduplicationRunner.class);
    @Autowired
    private DeduplicationDao deduplicationDao;
    @Autowired
    private BioSamplesClient bioSamplesClient;
    private final PipelinesProperties pipelinesProperties;

    public DeduplicationRunner(PipelinesProperties pipelinesProperties) {
        this.pipelinesProperties = pipelinesProperties;
    }

    @Override
    public void run(final ApplicationArguments args) {
        final List<DeduplicationDao.RowMapping> mappingList = deduplicationDao.getAllSamples();
        boolean isPassed = true;

        try (AdaptiveThreadPoolExecutor executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true,
                pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax())) {

            mappingList.forEach(pair -> executorService.submit(() -> checkDuplicates(pair)));
        } catch (final Exception e) {
            log.error("Pipeline failed to finish successfully", e);
            isPassed = false;
        } finally {
            MailSender.sendEmail("De-duplication pipeline", null, isPassed);
            log.info("Completed de-duplicaion pipeline");
        }
    }

    private void checkDuplicates(final DeduplicationDao.RowMapping pair) {
        final String enaId = pair.getEnaId();
        final Iterator<Resource<Sample>> it = bioSamplesClient.fetchSampleResourceAll(enaId).iterator();
        final List<Sample> enaAeSamples = evaluateIterablesToFindPair(it, pair);

        Sample enaSample = null, aeSample = null;
        int enaAeSamplesCount = enaAeSamples.size();

        if (enaAeSamplesCount == 0) {
            log.info("No sample for this ERS " + enaId);
        } else if (enaAeSamplesCount == 1) {
            log.info("Not the ENA-AE duplication case, only 1 sample in BSD for this ERS " + enaId);
        } else if (enaAeSamplesCount == 2) {
            log.info("ENA-AE Duplicate found " + enaId);
            for (Sample sample : enaAeSamples) {
                if (sample.getAccession().equals(pair.getBioSampleId())) {
                    enaSample = sample;
                } else {
                    aeSample = sample;
                }
            }

            assert enaSample != null;
            assert aeSample != null;

            if (enaSample.getRelease().isAfter(Instant.now()) || aeSample.getRelease().isAfter(Instant.now()))
                log.info("Already set to private, no action required " + enaId);
            else mergeSamples(enaSample, aeSample);
        } else {
            log.info("More than 2 samples fetched for the ERS " + enaId);

            for (Sample sample : enaAeSamples) {
                if (sample.getAccession().equals(pair.getBioSampleId())) {
                    enaSample = sample;
                }
            }

            if (enaSample != null) {
                enaAeSamples.remove(enaSample);
                mergeSamples(enaSample, enaAeSamples);
            }
        }
    }

    private List<Sample> evaluateIterablesToFindPair(Iterator<Resource<Sample>> it, DeduplicationDao.RowMapping pair) {
        final List<Sample> enaAeSamples = new ArrayList<>();

        while (it.hasNext()) {
            final Sample sample = it.next().getContent();

            if (sample.getAccession().equals(pair.getBioSampleId())) {
                enaAeSamples.add(sample);
            } else if (sample.getExternalReferences().stream().anyMatch(extR -> extR.getUrl().contains("arrayexpress") || extR.getUrl().contains(pair.getEnaId()))) {
                enaAeSamples.add(sample);
            }
        }

        return enaAeSamples;
    }

    private void mergeSamples(final Sample enaSample, final Sample aeSample) {
        mergeAttributesAndSubmit(enaSample, aeSample);
    }

    private void mergeSamples(final Sample enaSample, final List<Sample> aeSamples) {
        mergeAttributesAndSubmit(enaSample, aeSamples);
    }

    private void mergeAttributesAndSubmit(final Sample enaSample, final Sample aeSample) {
        Sample sampleToSave;
        Sample sampleToPrivate;

        sampleToSave = Sample.Builder.fromSample(enaSample).withAttributes(resolveAttributes(enaSample.getAttributes(), aeSample.getAttributes())).build();
        sampleToPrivate = Sample.Builder.fromSample(aeSample).withRelease(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant()).build();

        bioSamplesClient.persistSampleResource(sampleToSave);
        log.info("Submitted sample with accession - " + sampleToSave.getAccession());

        bioSamplesClient.persistSampleResource(sampleToPrivate);
        log.info("Private sample with accession - " + sampleToPrivate.getAccession());
    }

    private void mergeAttributesAndSubmit(final Sample enaSample, final List<Sample> aeSamples) {
        try {
            Sample sampleToSave = Sample.Builder.fromSample(enaSample).build();

            bioSamplesClient.persistSampleResource(sampleToSave);
            log.info("Submitted sample with accession - " + sampleToSave.getAccession());

            aeSamples.forEach(sample -> {
                Sample sampleToPrivate = Sample.Builder.fromSample(sample).withRelease(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant()).build();

                bioSamplesClient.persistSampleResource(sampleToPrivate);
                log.info("Private sample with accession - " + sampleToPrivate.getAccession());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Set<Attribute> resolveAttributes(final SortedSet<Attribute> enaSample, final SortedSet<Attribute> aeSample) {
        final Set<Attribute> setOfAttributes = new HashSet<>(enaSample);

        enaSample.forEach(attrFirst -> aeSample.forEach(attrSecond -> {
            if (attrFirst.getType().equalsIgnoreCase(attrSecond.getType())) {
                if (!attrFirst.getValue().equalsIgnoreCase(attrSecond.getValue())) {
                    setOfAttributes.add(attrSecond);
                }
            } else {
                setOfAttributes.add(attrSecond);
            }
        }));

        return setOfAttributes;
    }
}
