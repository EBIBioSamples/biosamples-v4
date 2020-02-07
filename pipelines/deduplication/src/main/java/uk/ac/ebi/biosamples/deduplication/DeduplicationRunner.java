package uk.ac.ebi.biosamples.deduplication;

import io.reactivex.Observable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Component
public class DeduplicationRunner implements ApplicationRunner {
    @Autowired
    private DeduplicationDao deduplicationDao;

    @Autowired
    private BioSamplesClient bioSamplesClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<DeduplicationDao.RowMapping> mappingList = deduplicationDao.getAllSamples();
        Observable<DeduplicationDao.RowMapping> observable = Observable.fromIterable(mappingList);
        observable.subscribe(mapping -> checkDuplicates(mapping));
    }

    private void checkDuplicates(DeduplicationDao.RowMapping pair) {
        List<Filter> filterList = new ArrayList<>(2);
        filterList.add(FilterBuilder.create().onAttribute(pair.getEnaId()).build());
        System.out.println("Query for " + pair.getEnaId());
        long count = StreamSupport.stream(bioSamplesClient.fetchSampleResourceAll(null, filterList).spliterator(), false).count();
        System.out.println(count);

        if(count > 1) {
            System.out.println(pair.getEnaId());
        }
    }
}
