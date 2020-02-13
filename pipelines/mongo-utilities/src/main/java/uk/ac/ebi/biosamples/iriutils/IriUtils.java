/*
package uk.ac.ebi.biosamples.iriutils;

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
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class IriUtils implements ApplicationRunner {
    private Logger log = LoggerFactory.getLogger(IriUtils.class);
    @Autowired
    private BioSamplesClient bioSamplesClient;
    Long totalIri = new Long(0);
    Long totalOneIriSample = new Long(0);
    Long totalTwoIriSample = new Long(0);

    @Override
    public void run(final ApplicationArguments args) {
        List<Filter> filterList = new ArrayList<>();
        filterList.add(FilterBuilder.create().onReleaseDate().from("2000-01-01").until("2005-01-01").build());
        final Observable<Resource<Sample>> observable1 = Observable.fromIterable(bioSamplesClient.fetchSampleResourceAll(filterList));
        observable1.subscribe(this::countIris);

        */
/*List<Sample> sampleList = new ArrayList<>();
        Iterator<Resource<Sample>> iterator = bioSamplesClient.fetchSampleResourceAll().iterator();

        while (iterator.hasNext()) {
            sampleList.add(iterator.next().getContent());
        }

        log.info(String.valueOf(sampleList.size()));*//*


        log.info("One iri sample " + totalOneIriSample);
        log.info("Two iri sample " + totalTwoIriSample);
        log.info("Total iris " + totalIri);
    }

    private void countIris(Resource<Sample> sample) {
        sample.getContent().getAttributes().forEach(attr -> {
            int numberOfIriInASample = 0;

            if (attr.getIri() != null && attr.getIri().size() > 0) {
                totalIri++;
                numberOfIriInASample++;
            }

            if (numberOfIriInASample > 1) {
                totalTwoIriSample++;
                if (totalTwoIriSample % 50 == 0) {
                    log.info("Two iri sample present count is " + totalTwoIriSample);
                }
            } else if (numberOfIriInASample == 1) {
                totalOneIriSample++;
                if (totalOneIriSample % 50 == 0) {
                    log.info("One iri sample present count is " + totalOneIriSample);
                }
            }
        });
    }
}

*/
