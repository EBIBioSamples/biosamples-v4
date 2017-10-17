package uk.ac.ebi.biosamples.solr.service;

import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.filters.*;
import uk.ac.ebi.biosamples.service.FacetToFilterConverter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
//        String filterTargetField = solrFieldService.encodedField(filter.getLabel(), facetFilterConverter.convert(type));
        Criteria filterCriteria = null;
        switch(type) {
            case ATTRIBUTE_FILTER:
            case RELATION_FILER:
            case INVERSE_RELATION_FILTER:
                String filterTargetField = solrFieldService.encodedField(filter.getLabel(), facetFilterConverter.convert(type));
                if (content instanceof EmptyFilter) {
                    filterCriteria = new Criteria(filterTargetField).isNotNull();
                } else {
                    ValueFilter valueContent = (ValueFilter) content;
                    filterCriteria = new Criteria(filterTargetField).is(valueContent.getContent());
                }
                break;
            case DATE_FILTER:
                if (content instanceof EmptyFilter) {
                    filterCriteria = new Criteria(filter.getLabel()).isNotNull();
                } else {
                    // I have to split manually the different queries
                    DateRangeFilterContent.DateRange dateRange = ((DateRangeFilterContent) content).getContent();
                    filterCriteria = new Criteria(filter.getLabel());
                    if (dateRange.isFromMinDate() && dateRange.isToMaxDate()) {
                        filterCriteria = filterCriteria.isNotNull();
                    } else if (dateRange.isFromMinDate()) {
                        filterCriteria = filterCriteria.lessThanEqual(getSolrCompatibleDate(dateRange.getTo()));
                    } else if (dateRange.isToMaxDate()){
                        filterCriteria = filterCriteria.greaterThanEqual(getSolrCompatibleDate(dateRange.getFrom()));
                    } else {
                        filterCriteria = filterCriteria.between(
                                dateRange.getFrom().format(DateTimeFormatter.ISO_INSTANT),
                                dateRange.getTo().format(DateTimeFormatter.ISO_INSTANT));
                    }
                }
                break;
        }
        return Optional.ofNullable(filterCriteria);

    }

    /**
     * Return an optional list of criterias based on filters with same type and label of a reference filter
     * @param availableFilters the list of filters to scan
     * @param referenceFilter
     * @return Optional List of criteria
     */
    public Optional<List<Filter>> getCompatibleFilters(List<Filter> availableFilters, Filter referenceFilter) {
        List<Filter> compatibleFilterList = new ArrayList<>();
        for (Filter nextFilter : availableFilters) {
            if (nextFilter.getLabel().equals(referenceFilter.getLabel()) && nextFilter.getKind().equals(referenceFilter.getKind())) {
                compatibleFilterList.add(nextFilter);
            }
        }
        if(compatibleFilterList.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(compatibleFilterList);

    }

    /**
     * Produce a filter query based on the provided filters
     * @param filters a collection of filters
     * @return the corresponding filter query
     */
    public Optional<FilterQuery> getFilterQuery(Collection<Filter> filters) {
        if (filters == null || filters.size() == 0) {
            return Optional.empty();
        }

        boolean filterActive = false;
        FilterQuery filterQuery = new SimpleFilterQuery();
        List<Filter> filtersBag = new ArrayList<>(filters);
        while(!filtersBag.isEmpty()) {
            Filter currentFilter = filtersBag.remove(0);
            Optional<Criteria> optionalFilterCriteria = getFilterCriteria(currentFilter);
            if (optionalFilterCriteria.isPresent()) {
                Criteria criteria = optionalFilterCriteria.get();
                Optional<List<Filter>> optionalCompatibleFilters = getCompatibleFilters(filtersBag, currentFilter);
                optionalCompatibleFilters.ifPresent(compatibleFilterList -> {
                    for (Filter filter : compatibleFilterList) {
                        Optional<Criteria> orCriteria = getFilterCriteria(filter);
                        orCriteria.ifPresent(criteria::or);
                    }
                    filtersBag.removeAll(compatibleFilterList);
                });
                filterQuery.addCriteria(criteria);
                if (!filterActive) {
                    filterActive = true;
                }
            }

        }
        if (filterActive) {
            return Optional.of(filterQuery);
        }
        return Optional.empty();

    }

    /**
     * Return a filter query for public samples (released in the past) or samples
     * part of the provided domains
     * @param domains a collection of domains
     * @return a filter query for public and domain relevant samples
     */
    public Optional<FilterQuery> getPublicFilterQuery(Collection<String> domains) {
        //check if this is a read superuser
        if (domains.contains(bioSamplesProperties.getBiosamplesAapSuperRead())) {
            return Optional.empty();
        }

        //filter out non-public
        FilterQuery filterQuery = new SimpleFilterQuery();
        Criteria publicSampleCriteria = new Criteria("release_dt").lessThan("NOW").and("release_dt").isNotNull();

        if (!domains.isEmpty()) {
            //user can only see private samples inside its own domain
            publicSampleCriteria.or(new Criteria("domain_s").in(domains));
        }

        filterQuery.addCriteria(publicSampleCriteria);
        return Optional.of(filterQuery);


    }


    public String getSolrCompatibleDate(ZonedDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")));
    }

}
