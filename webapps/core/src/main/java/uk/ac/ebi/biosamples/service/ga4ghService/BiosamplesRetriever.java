package uk.ac.ebi.biosamples.service.ga4ghService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.ga4ghmetadata.*;
import uk.ac.ebi.biosamples.service.FilterBuilder;


import java.util.*;


@Component
@Scope("prototype")
public class BiosamplesRetriever {

    private BioSamplesClient client;
    private FilterCreator filterCreator;
    @Autowired
    protected BiosampleToGA4GHMapper mapper;

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

    public List<Biosample> getFilteredSamplesBySearchForm(SearchingForm form) {
        Collection<Collection<Filter>> filters = filterCreator.createFilters(form);
        List<Biosample> results = new ArrayList<>();
        for (Collection<Filter> filter : filters) {
            Iterable<Resource<Sample>> samples = client.fetchSampleResourceAll(form.getText(), filter);
            for(Resource<Sample> resource:samples){
                Sample sample = resource.getContent();
                Biosample biosample = mapper.mapSampleToGA4GH(sample);
                results.add(biosample);
            }


        }

        return results;
    }

}

