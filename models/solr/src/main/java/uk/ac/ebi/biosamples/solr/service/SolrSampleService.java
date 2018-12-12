package uk.ac.ebi.biosamples.solr.service;

import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

import java.util.*;

@Service
public class SolrSampleService {

    private final SolrSampleRepository solrSampleRepository;

    private final BioSamplesProperties bioSamplesProperties;

    private final SolrFacetService solrFacetService;
    private final SolrFieldService solrFieldService;
    private final SolrFilterService solrFilterService;

    //maximum time allowed for a solr search in s
    //TODO application.properties this
    private static final int TIMEALLOWED = 30;

    private Logger log = LoggerFactory.getLogger(getClass());

    public SolrSampleService(SolrSampleRepository solrSampleRepository,
                             BioSamplesProperties bioSamplesProperties,
                             SolrFacetService solrFacetService,
                             SolrFieldService solrFieldService,
                             SolrFilterService solrFilterService) {
        this.solrSampleRepository = solrSampleRepository;
        this.bioSamplesProperties = bioSamplesProperties;
        this.solrFacetService = solrFacetService;
        this.solrFieldService = solrFieldService;
        this.solrFilterService = solrFilterService;
    }

    /**
     * Fetch the solr samples based on query specification
     *
     * @param searchTerm the term to search for in solr
     * @param filters    a Collection of filters used in the solr query
     * @param domains    a Collection of domains used in the solr query
     * @param pageable   pagination information
     * @return a page of Samples full-filling the query
     */
    public Page<SolrSample> fetchSolrSampleByText(String searchTerm, Collection<Filter> filters,
                                                  Collection<String> domains, Pageable pageable) {
        Page<SolrSample> result;
        try {
            Query query = buildQuery(searchTerm, filters, domains);
            query.setPageRequest(pageable);
            query.setTimeAllowed(TIMEALLOWED * 1000);
            // return the samples from solr that match the query
            result = solrSampleRepository.findByQuery(query);
        } catch (Exception e) {
            //If it is not possible to use the search as a filter treat search string as text
            String escapedSearchTerm = searchTerm == null ? null : ClientUtils.escapeQueryChars(searchTerm);
            Query query = buildQuery(escapedSearchTerm, filters, domains);
            query.setPageRequest(pageable);
            query.setTimeAllowed(TIMEALLOWED * 1000);
            // return the samples from solr that match the query
            result = solrSampleRepository.findByQuery(query);
        }
        return result;
    }

    /**
     * Fetch the solr samples based on query specification
     *
     * @param searchTerm the term to search for in solr
     * @param filters    a Collection of filters used in the solr query
     * @param domains    a Collection of domains used in the solr query
     * @param cursorMark cursor serialization
     * @return a page of Samples full-filling the query
     */
    public CursorArrayList<SolrSample> fetchSolrSampleByText(String searchTerm, Collection<Filter> filters,
                                                             Collection<String> domains, String cursorMark, int size) {
        Query query = buildQuery(searchTerm, filters, domains);
        query.addSort(new Sort("id")); //this must match the field in solr

        return solrSampleRepository.findByQueryCursorMark(query, cursorMark, size);
    }

    private Query buildQuery(String searchTerm, Collection<Filter> filters, Collection<String> domains) {

        //default to search all
        if (searchTerm == null || searchTerm.trim().length() == 0) {
            searchTerm = "*:*";
        }
        //build a query out of the users string and any facets
        Query query = new SimpleQuery(searchTerm);
        query.addProjectionOnField(new SimpleField("id"));

        Optional<FilterQuery> publicFilterQuery = solrFilterService.getPublicFilterQuery(domains);
        publicFilterQuery.ifPresent(query::addFilterQuery);

        Optional<FilterQuery> optionalFilter = solrFilterService.getFilterQuery(filters);
        optionalFilter.ifPresent(query::addFilterQuery);

        return query;
    }

    public Autocomplete getAutocomplete(String autocompletePrefix, Collection<Filter> filters, int maxSuggestions) {

        //default to search all
        String searchTerm = "*:*";
        //build a query out of the users string and any facets
        FacetQuery query = new SimpleFacetQuery();
        query.addCriteria(new Criteria().expression(searchTerm));
        query.addProjectionOnField(new SimpleField("id"));

        Optional<FilterQuery> optionalFilter = solrFilterService.getFilterQuery(filters);
        optionalFilter.ifPresent(query::addFilterQuery);

        //filter out non-public
        Optional<FilterQuery> publicSampleFilterQuery = solrFilterService.getPublicFilterQuery(Collections.EMPTY_LIST);
        publicSampleFilterQuery.ifPresent(query::addFilterQuery);

        query.setPageRequest(new PageRequest(0, 1));

        FacetOptions facetOptions = new FacetOptions();
        facetOptions.addFacetOnField("autocomplete_ss");
        facetOptions.setPageable(new PageRequest(0, maxSuggestions));
        facetOptions.setFacetPrefix(autocompletePrefix);
        query.setFacetOptions(facetOptions);
        query.setTimeAllowed(TIMEALLOWED * 1000);

        FacetPage<?> facetPage = solrSampleRepository.findByFacetQuery(query);

        Page<FacetFieldEntry> facetFiledEntryPage = facetPage.getFacetResultPage("autocomplete_ss");

        List<String> autocompleted = new ArrayList<>();
        for (FacetFieldEntry facetFieldEntry : facetFiledEntryPage) {
            autocompleted.add(facetFieldEntry.getValue());
        }
        return new Autocomplete(autocompletePrefix, autocompleted);
    }

}
