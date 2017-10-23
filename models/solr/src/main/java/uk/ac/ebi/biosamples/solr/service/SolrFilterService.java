package uk.ac.ebi.biosamples.solr.service;

import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.filters.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filters.Filter;
import uk.ac.ebi.biosamples.service.FacetToFilterConverter;
import uk.ac.ebi.biosamples.solr.model.field.SolrFieldType;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static uk.ac.ebi.biosamples.model.filters.DateRangeFilter.DateRange;

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

        //TODO rename to getFilterTargetField
        String filterTargetField = solrFieldService.encodedField(filter.getLabel(), SolrFieldType.getFromFilterType(filter.getType()));
        Criteria filterCriteria;
        if (filter.getContent().isPresent()) {
            Object content = filter.getContent().get();
            if (filter instanceof DateRangeFilter) {
                filterCriteria = getDateRangeCriteriaOnField(filterTargetField, (DateRange) content);
            } else {
                filterCriteria = new Criteria(filterTargetField).is(content);
            }
        } else {
            filterCriteria = new Criteria(filterTargetField).isNotNull();
        }
        return Optional.ofNullable(filterCriteria);

    }

    /**
     * Create a date range filter criteria on the specified field
     * @param fieldToFilter the field on which the criteria will be applied
     * @param dateRange the date range information
     * @return a new Criteria
     */
    private Criteria getDateRangeCriteriaOnField(String fieldToFilter, DateRange dateRange) {

        Criteria filterCriteria = new Criteria(fieldToFilter);
        if (dateRange.isFromMinDate() && dateRange.isUntilMaxDate()) {
            filterCriteria = filterCriteria.isNotNull();
        } else if (dateRange.isFromMinDate()) {
            filterCriteria = filterCriteria.lessThanEqual(toSolrDateString(dateRange.getUntil()));
        } else if (dateRange.isUntilMaxDate()){
            filterCriteria = filterCriteria.greaterThanEqual(toSolrDateString(dateRange.getFrom()));
        } else {
            filterCriteria = filterCriteria.between(
                    dateRange.getFrom().format(DateTimeFormatter.ISO_INSTANT),
                    dateRange.getUntil().format(DateTimeFormatter.ISO_INSTANT));
        }
        return filterCriteria;
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
            if (nextFilter.getLabel().equals(referenceFilter.getLabel()) && nextFilter.getType().equals(referenceFilter.getType())) {
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


    public String toSolrDateString(ZonedDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")));
    }

}
