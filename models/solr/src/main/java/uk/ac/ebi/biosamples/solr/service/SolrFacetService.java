package uk.ac.ebi.biosamples.solr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.facets.*;
import uk.ac.ebi.biosamples.model.filters.Filter;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.Entry;
import static uk.ac.ebi.biosamples.solr.service.SolrFieldService.FieldInfo;

@Service
public class SolrFacetService {

    private static final int TIMEALLOWED = 30;
    private final SolrSampleRepository solrSampleRepository;
    private final SolrFieldService solrFieldService;
    private Logger log = LoggerFactory.getLogger(getClass());
    private final SolrFilterService solrFilterService;

    public SolrFacetService(SolrSampleRepository solrSampleRepository, SolrFieldService solrFieldService, SolrFilterService solrFilterService, BioSamplesProperties bioSamplesProperties) {
        this.solrSampleRepository = solrSampleRepository;
        this.solrFieldService = solrFieldService;
        this.solrFilterService = solrFilterService;
    }


    public List<Facet> getFacets(String searchTerm,
                                 Collection<Filter> filters,
                                 Collection<String> domains,
                                 Pageable facetFieldPageInfo,
                                 Pageable facetValuesPageInfo) {
        //default to search all
        if (searchTerm == null || searchTerm.trim().length() == 0) {
            searchTerm = "*:*";
        }

        FacetsBuilder builder = new FacetsBuilder();

        //build a query out of the users string and any facets
        FacetQuery query = new SimpleFacetQuery();
        query.addCriteria(new Criteria().expression(searchTerm));
        query.setTimeAllowed(TIMEALLOWED*1000);


        // Add domains and release date filters
        Optional<FilterQuery> domainAndPublicFilterQuery = solrFilterService.getPublicFilterQuery(domains);
        domainAndPublicFilterQuery.ifPresent(query::addFilterQuery);

        // Add all the provided filters
        Optional<FilterQuery> optionalFilter = solrFilterService.getFilterQuery(filters);
        optionalFilter.ifPresent(query::addFilterQuery);


        // Generate a facet query to get all the available facets for the samples
        Page<FacetFieldEntry> facetFields = solrSampleRepository.getFacetFields(query, facetFieldPageInfo);

        // Get the facet fields
        Map<String,Long> allFacetFields = new HashMap<>();
        for(FacetFieldEntry ffe: facetFields) {
            String facetField = ffe.getValue();
            Long facetFieldCount = ffe.getValueCount();
            allFacetFields.put(facetField, facetFieldCount);
        }

        /*
            Once I've the facet fields, I need to
            1. Get the type (_ir_ss, _av_ss, _dt, ...)
            2. Get the field name
         */
        List<FieldInfo> fieldInfoList = new ArrayList<>();
        for(Entry<String, Long> facetField: allFacetFields.entrySet()) {
            // Todo specify the object
            // TODO check if is possible to get the facet type from the solr field
            FieldInfo fieldInfo = solrFieldService.getFieldInfo(facetField);
            fieldInfoList.add(fieldInfo);
        }

        /*
            Group the facets by type so we can group also query to retrieve the facet values
            from Solr.
            FacetType should be a facet category to distinguish between the different
            kind of facet query we want to do: Regular facet queries, Range facet queries,
            interval facet queries or even pivot facet queries
        */
        MultiValueMap<FacetType, FieldInfo> groupedFacet = new LinkedMultiValueMap<>();
        for(FieldInfo fieldInfo: fieldInfoList) {
            groupedFacet.add(fieldInfo.getType(), fieldInfo);
        }

        /*
            Then based on the facet type I need to create a specific facet query
            1. _ir_ss => regular facet
            2. _av_ss => regular facet
            3. _dt => range facet
         */
        for (Entry<FacetType, List<FieldInfo>> entriesByType: groupedFacet.entrySet()) {
            // TODO Code smell - Too many switch cases
            switch(entriesByType.getKey()){
                case ATTRIBUTE:
                case INCOMING_RELATIONSHIP:
                case OUTGOING_RELATIONSHIP:
                    // TODO get a list of values for the facet field
                    List<Facet> regularFacets = getRegularFacets(query, entriesByType, facetValuesPageInfo);
                    regularFacets.forEach(builder::addFacet);
                    break;
                // TODO Implement all the other cases
                default:
                    throw new RuntimeException("No method available to retrieve facets values for facet type " + entriesByType.getKey());
            }
        }



        // Return the list of facets
        return builder.build();

    }

    /**
     * Get the regular facets using a solr sample repository;
     * Regular facets are the facets with a list of label-count entries
     * @param query the FacetQuery to apply
     * @param entriesByType an entry containing the type of facet and the list of field to query for
     * @param facetPageable pagination information
     * @return
     */
    private List<Facet> getRegularFacets(FacetQuery query, Entry<FacetType, List<SolrFieldService.FieldInfo>> entriesByType, Pageable facetPageable) {

        // Get all the solr encoded fields I want to query solr for regular facets
        List<String> stringFacetFields = entriesByType.getValue().stream().map(FieldInfo::getEncodedField).collect(Collectors.toList());
        FacetPage<?> facetPage = solrSampleRepository.getFacets(query, stringFacetFields, facetPageable);
        log.info("Got facet page for field type " + entriesByType.getKey().name());
        List<Facet> textFacets = new ArrayList<>();

        for (Field field : facetPage.getFacetFields()) {
            // Get the field info associated with returned field from solr
            Optional<FieldInfo> optionalFieldInfo = entriesByType.getValue().stream()
                    .filter(info -> info.getEncodedField().equals(field.getName())).findFirst();

            if (!optionalFieldInfo.isPresent()) {
                throw new RuntimeException("Unexpected field returned when getting facets");
            }
            FieldInfo fieldInfo = optionalFieldInfo.get();

            // Create the list of facet value-count for the returned field
            List<LabelCountEntry> listFacetContent = new ArrayList<>();
            for (FacetFieldEntry ffe : facetPage.getFacetResultPage(field)) {
                log.info("Adding "+ fieldInfo.getFieldName() +" : "+ffe.getValue()+" with count "+ffe.getValueCount());
                listFacetContent.add(LabelCountEntry.build(ffe.getValue(), ffe.getValueCount()));
            }

            // Build the facet
            Facet facet = FacetFactory.build(fieldInfo.getType(), fieldInfo.getFieldName(), fieldInfo.getSampleCount(), new LabelCountListContent(listFacetContent));
            textFacets.add(facet);
        }

        return textFacets;
    }


}
