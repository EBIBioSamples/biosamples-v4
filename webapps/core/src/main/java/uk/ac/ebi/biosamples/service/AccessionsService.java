package uk.ac.ebi.biosamples.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;
import uk.ac.ebi.biosamples.utils.LinkUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class AccessionsService {
    private SolrSampleService solrSampleService;
    private FilterService filterService;

    public AccessionsService(SolrSampleService solrSampleService, FilterService filterService) {
        this.solrSampleService = solrSampleService;
        this.filterService = filterService;
    }

    public Page<String> getAccessions(final String text, final String[] requestfilters, final Integer page, final Integer size) {
        final PageRequest pageable = new PageRequest(page, size);
        final String decodedText = LinkUtils.decodeText(text);
        final String[] decodedFilter = LinkUtils.decodeTexts(requestfilters);
        final Collection<Filter> filtersAfterDecode = filterService.getFiltersCollection(decodedFilter);
        final List<String> accessions = new ArrayList<>();

        return fetchAccessions(pageable, decodedText, filtersAfterDecode, accessions);
    }

    private Page<String> fetchAccessions(final PageRequest pageable, final String decodedText, final Collection<Filter> filtersAfterDecode, final List<String> accessions) {
        final Page<SolrSample> pageSolrSample = solrSampleService.fetchSolrSampleByText(decodedText, filtersAfterDecode, Collections.EMPTY_LIST, pageable);
        return pageSolrSample.map(SolrSample::getAccession);
    }
}
