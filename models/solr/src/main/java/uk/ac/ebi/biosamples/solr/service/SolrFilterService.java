package uk.ac.ebi.biosamples.solr.service;

import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.filters.*;
import uk.ac.ebi.biosamples.service.FacetToFilterConverter;

import java.util.Collection;
import java.util.Optional;

@Service
public class SolrFilterService {

    private final SolrFieldService solrFieldService;
    private final FacetToFilterConverter facetFilterConverter;
    private final BioSamplesProperties bioSamplesProperties;

    public SolrFilterService(SolrFieldService solrFieldService, FacetToFilterConverter facetFilterConverter, BioSamplesProperties bioSamplesProperties) {
        this.solrFieldService = solrFieldService;
        this.facetFilterConverter = facetFilterConverter;
        this.bioSamplesProperties = bioSamplesProperties;
    }

    /**
     * Build a filter criteria based on the filter type and filter content
     * @param filter
     * @return an optional solr criteria for filtering purpose
     */
    public Optional<Criteria> getFilterCriteria(Filter filter) {
        //TODO implement the method
        FilterContent content = filter.getContent();
        FilterType type = filter.getKind();
        String filterTargetField = solrFieldService.encodedField(filter.getLabel(), facetFilterConverter.convert(type));
        Criteria filterCriteria = null;
        if (content instanceof EmptyFilter) {
            filterCriteria = new Criteria(filterTargetField).isNotNull();
        } else {
            switch(type) {
                case ATTRIBUTE_FILTER:
                case RELATION_FILER:
                case INVERSE_RELATION_FILTER:
                    ValueFilter valueContent = (ValueFilter) content;
                    for(String value: valueContent.getContent()) {
                        if (filterCriteria == null) {
                            filterCriteria = new Criteria(filterTargetField).is(value);
                        } else {
                            filterCriteria = filterCriteria.or(new Criteria(filterTargetField).is(value));
                        }
                    }

            }

        }
        return Optional.ofNullable(filterCriteria);

    }

    public Optional<FilterQuery> getFilterQuery(Collection<Filter> filters) {
        if (filters == null || filters.size() == 0) {
            return Optional.empty();
        }

        boolean filterActive = false;
        FilterQuery filterQuery = new SimpleFilterQuery();
        for(Filter filter: filters) {
            Optional<Criteria> optionalFilterCriteria = getFilterCriteria(filter);
            if (optionalFilterCriteria.isPresent()) {
                filterQuery.addCriteria(optionalFilterCriteria.get());
                filterActive = true;
            }

        }
        if (filterActive) {
            return Optional.of(filterQuery);
        }
        return Optional.empty();

    }

    public FilterQuery getPublicFilterQuery(Collection<String> domains) {
        //filter out non-public
        //filter to update date range
        FilterQuery filterQuery = new SimpleFilterQuery();
        //check if this is a read superuser
        if (!domains.contains(bioSamplesProperties.getBiosamplesAapSuperRead())) {
            //user can only see private samples inside its own domain
            filterQuery.addCriteria(new Criteria("release_dt").lessThan("NOW").and("release_dt").isNotNull()
                    .or(new Criteria("domain_s").in(domains)));
        }
        return filterQuery;

    }

}
