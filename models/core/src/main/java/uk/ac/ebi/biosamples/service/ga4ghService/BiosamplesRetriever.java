package uk.ac.ebi.biosamples.service.ga4ghService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.ga4gh_model.Ga4ghSample;
import uk.ac.ebi.biosamples.model.ga4gh_model.SearchingForm;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;


import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper class for biosamples client that retreives samples according specific (ga4gh) queries
 *
 * @author Dilshat Salikhov
 */

@Service
public class BiosamplesRetriever {

    private BioSamplesClient client;
    private GA4GHFilterCreator filterCreator;
    private SampleToGa4ghSampleConverter mapper;


    @Autowired
    public BiosamplesRetriever(BioSamplesClient bioSamplesClient, GA4GHFilterCreator filterCreator, SampleToGa4ghSampleConverter mapper) {
        this.client = bioSamplesClient;
        this.filterCreator = filterCreator;
        this.mapper = mapper;
    }

    public Sample getSampleById(String sampleID) throws NoSuchElementException {
        Optional<Resource<Sample>> optionalResource = client.fetchSampleResource(sampleID);
        Resource<Sample> sampleResource = optionalResource.get();
        return sampleResource.getContent();
    }


    public Iterable<Resource<Sample>> getFilteredSamplesByString(String text, List<String> rawFilters) {
        Collection<Filter> filters = new LinkedList<>();
        if (rawFilters != null) {
            for (String filter : rawFilters) {
                filters.add(FilterBuilder.create().buildFromString(filter));
            }
        }

        return client.fetchSampleResourceAll(text, filters);
    }

    public List<Ga4ghSample> getFilteredPagedSamplesBySearchForm(SearchingForm form, int page) {
        Collection<Collection<Filter>> filters = filterCreator.getFilters();
        String a = form.getText();
        Collection<Resource<Sample>> samples = new ArrayList<>();
        for (Collection<Filter> ga4ghFilters : filters) {
            PagedResources<Resource<Sample>> tempSamples = client.fetchPagedSampleResource(form.getText(), ga4ghFilters,page,10);
            Collection<Resource<Sample>> currentSamples = tempSamples.getContent();
             if (currentSamples != null) {
                samples.addAll(currentSamples);
            }

        }

        List<Ga4ghSample> ga4ghSamples = samples.parallelStream()
                .map(
                        i -> {
                            Sample sample = i.getContent();
                            return mapper.convert(sample);
                        }
                )
                .collect(Collectors.toList());
        return ga4ghSamples;
    }

}

