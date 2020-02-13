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

import java.io.IOException;
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
    public void run(final ApplicationArguments args) throws IOException {
        //makePrivateUtility();

        final List<DeduplicationDao.RowMapping> mappingList = deduplicationDao.getAllSamples();
        final Observable<DeduplicationDao.RowMapping> observable = Observable.fromIterable(mappingList);

        observable.subscribe(this::checkDuplicates);
    }

    /* Utility method to bulk private samples */
   /* private void makePrivateUtility() throws IOException {
        final List<String> privateSampleList = CsvReader.readCsv();

        log.info(String.valueOf(privateSampleList.size()));

        privateSampleList.forEach(sample -> {
            Optional<Resource<Sample>> sampleToBePrivate = bioSamplesClient.fetchSampleResource(sample);

            if(sampleToBePrivate.isPresent()) {
                final Sample privateSample = Sample.Builder.fromSample(sampleToBePrivate.get().getContent()).withRelease(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant()).build();
                bioSamplesClient.persistSampleResource(privateSample);
                log.info("Sample made private is " + privateSample.getAccession());
            }
        });
    }*/


    private void checkDuplicates(final DeduplicationDao.RowMapping pair) {
        final String enaId = pair.getEnaId();
        //List<Filter> filterList = new ArrayList<>(2);
        //filterList.add(FilterBuilder.create().onAttribute("SRA accession").withValue(pair.getEnaId()).build());
        final Iterator<Resource<Sample>> it = bioSamplesClient.fetchSampleResourceAll(enaId).iterator();
        Sample firstSample, secondSample;

        if (it.hasNext()) {
            firstSample = it.next().getContent();

            if (it.hasNext()) {
                secondSample = it.next().getContent();

                if (firstSample.getRelease().isAfter(Instant.now()) || secondSample.getRelease().isAfter(Instant.now()))
                    log.info("Already set to private, no action required");
                else {
                    mergeSamples(firstSample, secondSample, pair);
                }
            }
        } else {
            log.info("No sample for this ERS " + enaId);
        }
    }

    private void mergeSamples(final Sample firstSample, final Sample secondSample, final DeduplicationDao.RowMapping pair) {
        boolean useFirst = false;

        if (firstSample.getAccession().equals(pair.getBioSampleId())) {
            useFirst = true;
        }

        mergeAttributesAndSubmit(firstSample, secondSample, useFirst);
    }

    private void mergeAttributesAndSubmit(final Sample firstSample, final Sample secondSample, final boolean useFirst) {
        Sample sampleToSave;
        Sample sampleToPrivate;

        if (useFirst) {
            sampleToSave = Sample.Builder.fromSample(firstSample).withAttributes(resolveAttributes(firstSample.getAttributes(), secondSample.getAttributes())).build();
            sampleToPrivate = Sample.Builder.fromSample(secondSample).withRelease(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant()).build();
        } else {
            sampleToSave = Sample.Builder.fromSample(secondSample).withAttributes(resolveAttributes(firstSample.getAttributes(), secondSample.getAttributes())).build();
            sampleToPrivate = Sample.Builder.fromSample(firstSample).withRelease(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant()).build();
        }

        bioSamplesClient.persistSampleResource(sampleToSave);
        log.info("Submitted sample with accession - " + sampleToSave.getAccession());

        bioSamplesClient.persistSampleResource(sampleToPrivate);
        log.info("Private sample with accession - " + sampleToPrivate.getAccession());
    }

    private Set<Attribute> resolveAttributes(final SortedSet<Attribute> first, final SortedSet<Attribute> second) {
        final Set<Attribute> setOfAttributes = new HashSet<>();
        setOfAttributes.addAll(first);
        setOfAttributes.addAll(second);

        return setOfAttributes;
    }
}
