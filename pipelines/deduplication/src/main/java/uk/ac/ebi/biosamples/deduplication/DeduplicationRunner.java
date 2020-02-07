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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

@Component
public class DeduplicationRunner implements ApplicationRunner {
    private Logger log = LoggerFactory.getLogger(DeduplicationRunner.class);
    @Autowired
    private DeduplicationDao deduplicationDao;
    @Autowired
    private BioSamplesClient bioSamplesClient;

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        final List<DeduplicationDao.RowMapping> mappingList = deduplicationDao.getAllSamples();
        final Observable<DeduplicationDao.RowMapping> observable = Observable.fromIterable(mappingList);

        observable.subscribe(mapping -> checkDuplicates(mapping));
    }

    private void checkDuplicates(final DeduplicationDao.RowMapping pair) {
        final String enaId = pair.getEnaId();
        //List<Filter> filterList = new ArrayList<>(2);
        //filterList.add(FilterBuilder.create().onAttribute("SRA accession").withValue(pair.getEnaId()).build());
        final Iterator<Resource<Sample>> it = bioSamplesClient.fetchSampleResourceAll(enaId).iterator();
        Resource<Sample> first, second = null;

        if (it.hasNext()) {
            first = it.next();

            if (it.hasNext()) {
                second = it.next();
                mergeSamples(first, second, pair);
            }
        } else {
            log.info("No sample for this ERS ", enaId);
        }
    }

    private void mergeSamples(final Resource<Sample> first, final Resource<Sample> second, final DeduplicationDao.RowMapping pair) {
        final Sample firstSample = first.getContent();
        final Sample secondSample = second.getContent();
        SortedSet<Attribute> allAttributes;
        Sample sampleToSave = null;
        Sample sampleToPrivate = null;
        boolean useFirst = false;

        if(firstSample.getAccession().equals(pair.getBioSampleId())) {
            useFirst = true;
        }

        mergeAttributesAndSubmit(first.getContent(), second.getContent(), useFirst);
    }

    private void mergeAttributesAndSubmit(final Sample firstSample, final Sample secondSample, final boolean useFirst) {
        SortedSet<Attribute> allAttributes;
        Sample sampleToSave;
        Sample sampleToPrivate;
        allAttributes = resolveAttributes(firstSample, secondSample);

        if(useFirst) {
            sampleToSave = Sample.Builder.fromSample(firstSample).withAttributes(allAttributes).build();
            sampleToPrivate = Sample.Builder.fromSample(secondSample).withRelease(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant()).build();
        } else {
            sampleToSave = Sample.Builder.fromSample(secondSample).withAttributes(allAttributes).build();
            sampleToPrivate = Sample.Builder.fromSample(firstSample).withRelease(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant()).build();
        }

        bioSamplesClient.persistSampleResource(sampleToSave);
        log.info("Submitted sample " + sampleToSave.getAccession());
        bioSamplesClient.persistSampleResource(sampleToPrivate);
        log.info("Private sample " + sampleToPrivate.getAccession());
    }

    private SortedSet<Attribute> resolveAttributes(final Sample first, final Sample second) {
        final SortedSet<Attribute> firstAttributes = first.getAttributes();
        firstAttributes.addAll(second.getAttributes());

        return firstAttributes;
    }
}
