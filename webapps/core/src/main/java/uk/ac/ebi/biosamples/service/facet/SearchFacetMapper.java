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
package uk.ac.ebi.biosamples.service.facet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import uk.ac.ebi.biosamples.search.grpc.*;

public class SearchFacetMapper {

  public static List<Filter> getSearchFilters(
      Set<uk.ac.ebi.biosamples.core.model.filter.Filter> filters, String webinId) {
    List<Filter> grpcFilters = new ArrayList<>();
    grpcFilters.add(getPrivateSearchFilter(webinId));
    if (!CollectionUtils.isEmpty(filters)) {
      getSearchFilters(filters, grpcFilters);
    }
    return grpcFilters;
  }

  private static void getSearchFilters(
      Set<uk.ac.ebi.biosamples.core.model.filter.Filter> filters, List<Filter> grpcFilters) {
    for (uk.ac.ebi.biosamples.core.model.filter.Filter filter : filters) {
      Filter.Builder filterBuilder = Filter.newBuilder();
      if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.AccessionFilter f) {
        f.getContent()
            .ifPresent(
                accession ->
                    filterBuilder.setAccession(
                        AccessionFilter.newBuilder().setAccession(accession)));
      }
      if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.NameFilter f) {
        f.getContent()
            .ifPresent(name -> filterBuilder.setName(NameFilter.newBuilder().setName(name)));
      }
      if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.AuthenticationFilter f) {
        f.getContent()
            .ifPresent(
                auth -> { // todo domain
                  filterBuilder.setWebin(WebinIdFilter.newBuilder().setWebinId(auth));
                });
      }
      if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.DateRangeFilter f) {
        DateRangeFilter.DateField dateField =
            switch (f.getLabel()) {
              case "update" -> DateRangeFilter.DateField.UPDATE;
              case "create" -> DateRangeFilter.DateField.CREATE;
              case "release" -> DateRangeFilter.DateField.RELEASE;
              case "submitted" -> DateRangeFilter.DateField.SUBMITTED;
              default -> throw new IllegalArgumentException("Unknown date field " + f.getLabel());
            };

        f.getContent()
            .ifPresent(
                dateRange ->
                    filterBuilder.setDateRange(
                        DateRangeFilter.newBuilder()
                            .setField(dateField)
                            .setFrom(dateRange.getFrom().toString())
                            .setTo(dateRange.getUntil().toString())));
      }
      if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.AttributeFilter f) {
        AttributeFilter.Builder attributeFilterBuilder = AttributeFilter.newBuilder();
        attributeFilterBuilder.setField(f.getLabel());
        f.getContent()
            .ifPresent(
                attribute ->
                    attributeFilterBuilder.addAllValues(List.of(attribute))); // todo aggregation
        filterBuilder.setAttribute(attributeFilterBuilder);
      }
      if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.RelationFilter f) {
        RelationshipFilter.Builder relationshipFilterBuilder = RelationshipFilter.newBuilder();
        relationshipFilterBuilder.setType(f.getLabel());
        f.getContent().ifPresent(relationshipFilterBuilder::setTarget);
        filterBuilder.setRelationship(relationshipFilterBuilder);
      }
      if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.InverseRelationFilter f) {
        RelationshipFilter.Builder relationshipFilterBuilder = RelationshipFilter.newBuilder();
        relationshipFilterBuilder.setType(f.getLabel());
        f.getContent().ifPresent(relationshipFilterBuilder::setSource);
        filterBuilder.setRelationship(relationshipFilterBuilder);
      }
      if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.ExternalReferenceDataFilter f) {
        ExternalRefFilter.Builder externalRefFilterBuilder = ExternalRefFilter.newBuilder();
        externalRefFilterBuilder.setArchive(f.getLabel());
        f.getContent().ifPresent(externalRefFilterBuilder::setAccession);
        filterBuilder.setExternal(externalRefFilterBuilder);
      }

      // todo SraAccessionFilter, Structured data filter

      grpcFilters.add(filterBuilder.build());
    }
  }

  private static Filter getPrivateSearchFilter(String webinId) {
    PublicFilter.Builder publicFilterBuilder = PublicFilter.newBuilder();
    if (StringUtils.hasText(webinId)) {
      publicFilterBuilder.setWebinId(webinId);
    }
    return Filter.newBuilder().setPublic(publicFilterBuilder.build()).build();
  }
}
