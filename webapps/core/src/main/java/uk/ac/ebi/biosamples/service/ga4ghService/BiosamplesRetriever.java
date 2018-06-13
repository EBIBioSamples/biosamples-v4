package uk.ac.ebi.biosamples.service.ga4ghService;


import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ga4ghmetadata.Biosample;
import uk.ac.ebi.biosamples.ga4ghmetadata.SearchingForm;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.util.*;
import java.util.stream.Collectors;


@Component
public class BiosamplesRetriever {

    private BioSamplesClient client;
    private GA4GHFilterCreator filterCreator;
    private BiosampleToGA4GHMapper mapper;


    @Autowired
    BiosamplesRetriever(BioSamplesClient bioSamplesClient, GA4GHFilterCreator filterCreator, BiosampleToGA4GHMapper mapper) {
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

    public List<Biosample> getFilteredPagedSamplesBySearchForm(SearchingForm form, int page) {
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

        List<Biosample> biosamples = samples.parallelStream()
                .map(
                        i -> {
                            Sample sample = i.getContent();
                            return mapper.mapSampleToGA4GH(sample);
                        }
                )
                .collect(Collectors.toList());
        return biosamples;
    }

}

