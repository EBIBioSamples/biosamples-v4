package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private SolrSampleService solrSampleService;
    @Autowired
    private FilterService filterService;

    public List<String> getAccessions(final String text, final String[] requestfilters, final Integer page, final Integer size) {
        final PageRequest pageable = getPaginationDetails(page, size);
        final String decodedText = LinkUtils.decodeText(text);
        final String[] decodedFilter = LinkUtils.decodeTexts(requestfilters);
        final Collection<Filter> filtersAfterDecode = filterService.getFiltersCollection(decodedFilter);
        final List<String> accessions = new ArrayList<>();

        return fetchAccessions(pageable, decodedText, filtersAfterDecode, accessions);
    }

    private List<String> fetchAccessions(final PageRequest pageable, final String decodedText, final Collection<Filter> filtersAfterDecode, final List<String> accessions) {
        final Page<SolrSample> results = solrSampleService.fetchSolrSampleByText(decodedText, filtersAfterDecode, Collections.EMPTY_LIST, pageable);
        results.forEach(solrSample -> accessions.add(solrSample.getAccession()));

        return accessions;
    }

    private PageRequest getPaginationDetails(final Integer page, final Integer size) {
        int effectivePage;

        if (page == null) {
            effectivePage = 0;
        } else {
            effectivePage = page;
        }

        int effectiveSize;

        if (size == null) {
            effectiveSize = 20;
        } else {
            effectiveSize = size;
        }

        return new PageRequest(effectivePage, effectiveSize);
    }
}
