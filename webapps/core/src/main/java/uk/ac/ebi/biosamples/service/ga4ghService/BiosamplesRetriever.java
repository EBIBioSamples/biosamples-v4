package uk.ac.ebi.biosamples.service.ga4ghService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ga4ghmetadata.Biosample;
import uk.ac.ebi.biosamples.ga4ghmetadata.SearchingForm;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.service.ga4ghService.*;

import java.util.*;
import java.util.stream.Collectors;


@Component
public class BiosamplesRetriever {

    private BioSamplesClient client;
    private FilterCreator filterCreator;
    @Autowired
    protected BiosampleToGA4GHMapper mapper;
    protected PagedResources<Resource<Sample>> pagedSamples;


    @Autowired
    BiosamplesRetriever(BioSamplesClient bioSamplesClient, FilterCreator filterCreator) {
        this.client = bioSamplesClient;
        this.filterCreator = filterCreator;
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

    public List<Biosample> getFilteredSamplesBySearchForm(SearchingForm form, int page) {
        Collection<Collection<Filter>> filters = filterCreator.createFilters(form);
        pagedSamples = (PagedResources<Resource<Sample>>) PagedResources.NO_PAGE;
        Collection<Resource<Sample>> samples = new ArrayList<>();
        for(Collection<Filter> ga4ghFilters: filters){
            PagedResources<Resource<Sample>> tempSamples = client.fetchPagedSampleResource(form.getText(),ga4ghFilters,page,15);
            Collection<Resource<Sample>> currentSamples = tempSamples.getContent();
            if(currentSamples!=null) {
                samples.addAll(currentSamples);
            }
        }

        List<Biosample> biosamples = samples.parallelStream()
                .map(
                        i ->{ Sample sample = i.getContent();
                            return mapper.mapSampleToGA4GH(sample); }
                )
                .collect(Collectors.toList());
        return biosamples;
    }

}

