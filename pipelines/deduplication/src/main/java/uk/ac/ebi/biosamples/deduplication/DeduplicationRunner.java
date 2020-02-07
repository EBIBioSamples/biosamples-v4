package uk.ac.ebi.biosamples.deduplication;

import io.reactivex.Observable;
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
            System.out.println("No sample for this ERS " + enaId);
        }
    }

    private void mergeSamples(final Resource<Sample> first, final Resource<Sample> second, final DeduplicationDao.RowMapping pair) {
        final Sample firstSample = first.getContent();
        final Sample secondSample = second.getContent();
        Sample sampleToSave = null;
        Sample sampleToPrivate = null;
        boolean useFirst = false;

        if(firstSample.getAccession().equals(pair.getBioSampleId())) {
            useFirst = true;
        }

        SortedSet<Attribute> allAttributes = resolveAttributes(first, second);

        if(useFirst) {
            sampleToSave = Sample.Builder.fromSample(firstSample).withAttributes(allAttributes).build();
            sampleToPrivate = Sample.Builder.fromSample(secondSample).withRelease(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant()).build();
        } else {
            sampleToSave = Sample.Builder.fromSample(secondSample).withAttributes(allAttributes).build();
            sampleToPrivate = Sample.Builder.fromSample(firstSample).withRelease(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant()).build();
        }

        bioSamplesClient.persistSampleResource(sampleToSave);
        bioSamplesClient.persistSampleResource(sampleToPrivate);
    }

    private SortedSet<Attribute> resolveAttributes(final Resource<Sample> first, final Resource<Sample> second) {
        final SortedSet<Attribute> firstAttributes = first.getContent().getAttributes();
        firstAttributes.addAll(second.getContent().getAttributes());

        return firstAttributes;
    }
}
