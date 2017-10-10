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
import uk.ac.ebi.biosamples.model.facets.Facet;
import uk.ac.ebi.biosamples.model.facets.FacetType;
import uk.ac.ebi.biosamples.model.facets.FacetsBuilder;
import uk.ac.ebi.biosamples.model.facets.LabelCountEntry;
import uk.ac.ebi.biosamples.model.filters.Filter;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.Entry;

@Service
public class SolrFacetService {

    private static final int TIMEALLOWED = 30;
    private final SolrSampleRepository solrSampleRepository;
    private final SolrFieldService solrFieldService;
    private Logger log = LoggerFactory.getLogger(getClass());
    private final SolrFilterService solrFilterService;
    private final BioSamplesProperties bioSamplesProperties;

    public SolrFacetService(SolrSampleRepository solrSampleRepository, SolrFieldService solrFieldService, SolrFilterService solrFilterService, BioSamplesProperties bioSamplesProperties) {
        this.solrSampleRepository = solrSampleRepository;
        this.solrFieldService = solrFieldService;
        this.solrFilterService = solrFilterService;
        this.bioSamplesProperties = bioSamplesProperties;
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
        query.addFilterQuery(solrFilterService.getPublicFilterQuery(domains));
        query.setTimeAllowed(TIMEALLOWED*1000);

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
            FieldInfo fieldInfo = getFieldInfo(facetField);
            fieldInfoList.add(fieldInfo);
        }

        // Group the facets by type so we can group also query to retrieve the facet values
        // from Solr
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

    private List<Facet> getRegularFacets(FacetQuery query, Entry<FacetType, List<FieldInfo>> entriesByType, Pageable facetPageable) {
        List<String> stringFacetFields = entriesByType.getValue().stream().map(FieldInfo::getEncodedField).collect(Collectors.toList());
        FacetPage<?> facetPage = solrSampleRepository.getFacets(query, stringFacetFields, facetPageable);
        log.info("Got facet page for field type " + entriesByType.getKey().name());
        List<Facet> textFacets = new ArrayList<>();

        for (Field field : facetPage.getFacetFields()) {
            //for each value, put the number of them into this facets map
            Optional<FieldInfo> optionalFieldInfo = entriesByType.getValue().stream()
                    .filter(info -> info.encodedField.equals(field.getName())).findFirst();


            if (!optionalFieldInfo.isPresent()) {
                throw new RuntimeException("Unexpected field returned when getting facets");
            }

//            FacetType facetType = FacetType.ofField(field.getName());
//            String facetName = this.facetNameFromField(field.getName());
            FieldInfo fieldInfo = optionalFieldInfo.get();
            List<LabelCountEntry> listFacetContent = new ArrayList<>();
            for (FacetFieldEntry ffe : facetPage.getFacetResultPage(field)) {
                log.info("Adding "+ fieldInfo.getFieldName() +" : "+ffe.getValue()+" with count "+ffe.getValueCount());

                listFacetContent.add(LabelCountEntry.build(ffe.getValue(), ffe.getValueCount()));
            }
            Facet facet = Facet.build(fieldInfo.getType(), fieldInfo.getFieldName(), fieldInfo.getSampleCount(), listFacetContent);
            textFacets.add(facet);
        }

        return textFacets;
    }

    private FieldInfo getFieldInfo(Entry<String, Long> facetField) {
        // Read the suffix
        // Read the field
        String encodedFieldName = facetField.getKey();
        Long facetFieldSampleCount = facetField.getValue();
        Entry<FacetType, String> fieldTypeAndName = solrFieldService.decodeField(encodedFieldName);
        FacetType fieldType = fieldTypeAndName.getKey();
        String decodedFieldName = fieldTypeAndName.getValue();

        return new FieldInfo(encodedFieldName, decodedFieldName, fieldType, facetFieldSampleCount);
    }


    /*
        FieldInfo is in some way redundant at the moment

      */

    private class FieldInfo {
        private final FacetType type;
        private final String fieldName;
        private final String encodedField;
        private final Long sampleCount;


        FieldInfo(String encodedField,
                  String fieldName,
                  FacetType fieldType,
                  Long sampleCount) {
            this.type = fieldType;
            this.fieldName = fieldName;
            this.encodedField = encodedField;
            this.sampleCount = sampleCount;
        }

        public FacetType getType() {
            return type;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getEncodedField() {
            return encodedField;
        }

        public Long getSampleCount() {
            return sampleCount;
        }
    }
}
