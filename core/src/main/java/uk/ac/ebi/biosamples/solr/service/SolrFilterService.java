/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.solr.service;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.core.model.filter.AccessionFilter;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;

@Service
public class SolrFilterService {
  private final SolrFieldService solrFieldService;
  private final BioSamplesProperties bioSamplesProperties;

  public SolrFilterService(
      final SolrFieldService solrFieldService, final BioSamplesProperties bioSamplesProperties) {
    this.solrFieldService = solrFieldService;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  /**
   * Build a filter criteria based on the filter type and filter content
   *
   * @param filter
   * @return an optional solr criteria for filtering purpose
   */
  public Optional<Criteria> getFilterCriteria(final Filter filter) {
    // TODO rename to getFilterTargetField
    final SolrSampleField solrField = solrFieldService.getCompatibleField(filter);
    final Criteria filterCriteria = solrField.getFilterCriteria(filter);
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
      final List<Filter> availableFilters, final Filter referenceFilter) {
    final List<Filter> compatibleFilterList = new ArrayList<>();
    for (final Filter nextFilter : availableFilters) {
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
  List<FilterQuery> getFilterQuery(final Collection<Filter> filters) {
    if (filters == null || filters.size() == 0) {
      return Collections.emptyList();
    }

    boolean filterActive = false;
    final FilterQuery filterQuery = new SimpleFilterQuery();
    final List<FilterQuery> filterQueries = new ArrayList<>();
    final Collection<List<Filter>> filterGroups =
        filters.stream()
            .collect(
                Collectors.groupingBy(
                    filter -> new SimpleEntry(filter.getLabel(), filter.getType())))
            .values();
    for (final List<Filter> group : filterGroups) {
      // Compose all the or criteria available for the filters, if any available
      // Reduce will go through all criteria
      final boolean isAccessionFilter = group.stream().findFirst().get() instanceof AccessionFilter;
      final Optional<Criteria> filterCriteria =
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
                        return currentCriteria;
                      }
                    }
                    return Optional.empty();
                  });

      filterCriteria.ifPresent(criteria -> filterQueries.add(new SimpleFilterQuery(criteria)));
    }
    return filterQueries;
  }

  /**
   * Return a filter query for public samples (released in the past) or samples part of the
   * provided, and their own samples using auth domains
   *
   * @return a filter query for public and domain relevant samples
   */
  Optional<FilterQuery> getPublicFilterQuery(final String webinSubmissionAccountId) {
    // check if this is a read superuser
    if (webinSubmissionAccountId != null
        && webinSubmissionAccountId.equalsIgnoreCase(
            bioSamplesProperties.getBiosamplesClientWebinUsername())) {
      return Optional.empty();
    }

    // filter out non-public
    final FilterQuery filterQuery = new SimpleFilterQuery();
    Criteria publicSampleCriteria = new Criteria("release_dt").lessThan("NOW");

    //    publicSampleCriteria =
    //        publicSampleCriteria.and(
    //            new Criteria("status_s").not().in(SampleStatus.getSearchHiddenStatuses()));
    publicSampleCriteria =
        publicSampleCriteria.or(
            new Criteria(SolrFieldService.encodeFieldName("INSDC status") + "_av_ss")
                .not()
                .in(Collections.singletonList("suppressed")));

    if (webinSubmissionAccountId != null && !webinSubmissionAccountId.isEmpty()) {
      // user can see public and private samples submitted by them using their webin auth tokens

      publicSampleCriteria =
          publicSampleCriteria.or(new Criteria("webinId_s").is(webinSubmissionAccountId));
    }

    filterQuery.addCriteria(publicSampleCriteria);

    return Optional.of(filterQuery);
  }
}
