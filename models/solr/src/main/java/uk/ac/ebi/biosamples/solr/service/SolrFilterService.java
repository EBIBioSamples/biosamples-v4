/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.solr.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.filter.AccessionFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;

@Service
public class SolrFilterService {

  private final SolrFieldService solrFieldService;
  private final BioSamplesProperties bioSamplesProperties;

  private final DateTimeFormatter releaseFilterFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'23:59:59'Z'");

  public SolrFilterService(
      SolrFieldService solrFieldService, BioSamplesProperties bioSamplesProperties) {
    this.solrFieldService = solrFieldService;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  /**
   * Build a filter criteria based on the filter type and filter content
   *
   * @param filter
   * @return an optional solr criteria for filtering purpose
   */
  public Optional<Criteria> getFilterCriteria(Filter filter) {

    // TODO rename to getFilterTargetField
    SolrSampleField solrField = solrFieldService.getCompatibleField(filter);
    Criteria filterCriteria = solrField.getFilterCriteria(filter);
    return Optional.ofNullable(filterCriteria);
  }

  /**
   * Return an optional list of criterias based on filters with same type and label of a reference
   * filter
   *
   * @param availableFilters the list of filters to scan
   * @param referenceFilter
   * @return Optional List of criteria
   */
  public Optional<List<Filter>> getCompatibleFilters(
      List<Filter> availableFilters, Filter referenceFilter) {
    List<Filter> compatibleFilterList = new ArrayList<>();
    for (Filter nextFilter : availableFilters) {
      if (nextFilter.getLabel().equals(referenceFilter.getLabel())
          && nextFilter.getType().equals(referenceFilter.getType())) {
        compatibleFilterList.add(nextFilter);
      }
    }
    if (compatibleFilterList.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(compatibleFilterList);
  }

  /**
   * Produce a filter query based on the provided filters
   *
   * @param filters a collection of filters
   * @return the corresponding filter query
   */
  public Optional<FilterQuery> getFilterQuery(Collection<Filter> filters) {
    if (filters == null || filters.size() == 0) {
      return Optional.empty();
    }

    boolean filterActive = false;
    FilterQuery filterQuery = new SimpleFilterQuery();
    Collection<List<Filter>> filterGroups =
        filters.stream()
            .collect(
                Collectors.groupingBy(
                    filter -> new SimpleEntry(filter.getLabel(), filter.getType())))
            .values();
    for (List<Filter> group : filterGroups) {
      // Compose all the or criteria available for the filters, if any available
      // Reduce will go through all criteria
      boolean isAccessionFilter = group.stream().findFirst().get() instanceof AccessionFilter;
      Optional<Criteria> filterCriteria =
          group.stream()
              .map(this::getFilterCriteria)
              .reduce(
                  Optional.empty(),
                  (composedCriteria, currentCriteria) -> {
                    if (currentCriteria.isPresent()) {
                      if (composedCriteria.isPresent()) {
                        // Compose with an OR
                        if (isAccessionFilter) {
                          return Optional.of(composedCriteria.get().and(currentCriteria.get()));
                        } else {
                          return Optional.of(composedCriteria.get().or(currentCriteria.get()));
                        }
                      } else {
                        // Create a new criteria
                        return Optional.of(currentCriteria.get());
                      }
                    }
                    return Optional.empty();
                  });

      if (filterCriteria.isPresent()) {
        filterActive = true;
        filterQuery.addCriteria(filterCriteria.get());
      }
    }
    if (filterActive) {
      return Optional.of(filterQuery);
    }
    return Optional.empty();
  }

  /**
   * Return a filter query for public samples (released in the past) or samples part of the provided
   * domains
   *
   * @param domains a collection of domains
   * @return a filter query for public and domain relevant samples
   */
  public Optional<FilterQuery> getPublicFilterQuery(Collection<String> domains) {
    // check if this is a read superuser
    if (domains.contains(bioSamplesProperties.getBiosamplesAapSuperRead())) {
      return Optional.empty();
    }

    // filter out non-public
    // use a day-based time so the filter is cacheable
    String dateTime = releaseFilterFormatter.format(ZonedDateTime.now(ZoneOffset.UTC));
    FilterQuery filterQuery = new SimpleFilterQuery();
    Criteria publicSampleCriteria = new Criteria("release_dt").lessThan("NOW/DAY");
    // can use .and("release_dt").isNotNull(); to filter out non-null
    // but nothing should be null and this slows search

    if (!domains.isEmpty()) {
      // user can only see private samples inside its own domain
      // TODO fix integration tests for this
      // publicSampleCriteria = publicSampleCriteria.or(new Criteria("domain_s").in(domains));
    }

    filterQuery.addCriteria(publicSampleCriteria);
    return Optional.of(filterQuery);
  }
}
