package uk.ac.ebi.biosamples.deduplication;

import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

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

    @Override
    public void run(final ApplicationArguments args) {
        final List<DeduplicationDao.RowMapping> mappingList = deduplicationDao.getAllSamples();
        final Observable<DeduplicationDao.RowMapping> observable = Observable.fromIterable(mappingList);

        observable.subscribe(this::checkDuplicates);
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
            log.info("Not the ENA-AE duplication case");
        } else if (enaAeSamplesCount == 2) {
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
                log.info("Already set to private, no action required");
            else mergeSamples(enaSample, aeSample);
        } else {
            log.info("More than 2 samples fetched");
        }
    }

    private List<Sample> evaluateIterablesToFindPair(Iterator<Resource<Sample>> it, DeduplicationDao.RowMapping pair) {
        final List<Sample> enaAeSamples = new ArrayList<>(2);

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
