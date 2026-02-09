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
package uk.ac.ebi.biosamples.service.search;

import java.util.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import uk.ac.ebi.biosamples.core.model.filter.AuthenticationFilter;
import uk.ac.ebi.biosamples.core.model.filter.ExternalReferenceDataFilter;
import uk.ac.ebi.biosamples.core.model.filter.InverseRelationFilter;
import uk.ac.ebi.biosamples.core.model.filter.RelationFilter;
import uk.ac.ebi.biosamples.core.model.filter.StructuredDataFilter;
import uk.ac.ebi.biosamples.search.grpc.*;

public class SearchFilterMapper {

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
    Map<String, Filter.Builder> filterMap = new HashMap<>();
    for (uk.ac.ebi.biosamples.core.model.filter.Filter filter : filters) {
      if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.AccessionFilter f) {
        getAccessionSearchFilter(f, grpcFilters);
      } else if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.NameFilter f) {
        getNameSearchFilter(f, grpcFilters);
      } else if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.AuthenticationFilter f) {
        getAuthSearchFilter(f, grpcFilters);
      } else if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.DateRangeFilter f) {
        getDateRangeSearchFilter(f, grpcFilters);
      } else if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.AttributeFilter f) {
        getAttributeSearchFilter(f, filterMap);
      } else if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.RelationFilter f) {
        getRelationshipSearchFilter(f, grpcFilters);
      } else if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.InverseRelationFilter f) {
        getInverseRelationshipSearchFilter(f, grpcFilters);
      } else if (filter
          instanceof uk.ac.ebi.biosamples.core.model.filter.ExternalReferenceDataFilter f) {
        getExternalReferenceSearchFilter(f, grpcFilters);
      } else if (filter instanceof uk.ac.ebi.biosamples.core.model.filter.StructuredDataFilter f) {
        getStructuredDataSearchFilter(f, grpcFilters);
      } else {
        // todo SraAccessionFilter
        throw new RuntimeException("Unsupported filter type " + filter.getClass().getName());
      }
    }
    filterMap.forEach((k, v) -> grpcFilters.add(v.build())); // allows OR filter for attributes
  }

  private static void getStructuredDataSearchFilter(
      StructuredDataFilter f, List<Filter> grpcFilters) {
    f.getContent()
        .filter(StringUtils::hasText)
        .ifPresent(
            dataType -> {
              grpcFilters.add(
                  Filter.newBuilder()
                      .setStructuredData(
                          uk.ac.ebi.biosamples.search.grpc.StructuredDataFilter.newBuilder()
                              .setType(dataType))
                      .build());
            });
  }

  private static void getAuthSearchFilter(AuthenticationFilter f, List<Filter> grpcFilters) {
    f.getContent()
        .ifPresent(
            auth -> { // todo domain
              grpcFilters.add(
                  Filter.newBuilder()
                      .setWebin(WebinIdFilter.newBuilder().setWebinId(auth))
                      .build());
            });
  }

  private static void getNameSearchFilter(
      uk.ac.ebi.biosamples.core.model.filter.NameFilter f, List<Filter> grpcFilters) {
    f.getContent()
        .ifPresent(
            name ->
                grpcFilters.add(
                    Filter.newBuilder().setName(NameFilter.newBuilder().setName(name)).build()));
  }

  private static void getAccessionSearchFilter(
      uk.ac.ebi.biosamples.core.model.filter.AccessionFilter f, List<Filter> grpcFilters) {
    f.getContent()
        .ifPresent(
            accession ->
                grpcFilters.add(
                    Filter.newBuilder()
                        .setAccession(AccessionFilter.newBuilder().setAccession(accession))
                        .build()));
  }

  private static void getExternalReferenceSearchFilter(
      ExternalReferenceDataFilter f, List<Filter> grpcFilters) {
    ExternalRefFilter.Builder externalRefFilterBuilder = ExternalRefFilter.newBuilder();
    f.getContent().ifPresent(externalRefFilterBuilder::setArchive);
    grpcFilters.add(Filter.newBuilder().setExternal(externalRefFilterBuilder).build());
  }

  private static void getInverseRelationshipSearchFilter(
      InverseRelationFilter f, List<Filter> grpcFilters) {
    RelationshipFilter.Builder relationshipFilterBuilder = RelationshipFilter.newBuilder();
    relationshipFilterBuilder.setType(f.getLabel());
    f.getContent().ifPresent(relationshipFilterBuilder::setSource);
    grpcFilters.add(Filter.newBuilder().setRelationship(relationshipFilterBuilder).build());
  }

  private static void getRelationshipSearchFilter(RelationFilter f, List<Filter> grpcFilters) {
    RelationshipFilter.Builder relationshipFilterBuilder = RelationshipFilter.newBuilder();
    if (f.getLabel().equalsIgnoreCase("relationship type")) {
      f.getContent().ifPresent(relationshipFilterBuilder::setType);
    } else if (f.getLabel().equalsIgnoreCase("relationship source")) {
      f.getContent().ifPresent(relationshipFilterBuilder::setSource);
    } else if (f.getLabel().equalsIgnoreCase("relationship target")) {
      f.getContent().ifPresent(relationshipFilterBuilder::setTarget);
    }

    grpcFilters.add(Filter.newBuilder().setRelationship(relationshipFilterBuilder).build());
  }

  private static void getDateRangeSearchFilter(
      uk.ac.ebi.biosamples.core.model.filter.DateRangeFilter f, List<Filter> grpcFilters) {
    f.getContent()
        .ifPresent(
            dateRange -> {
              DateRangeFilter.DateField dateField =
                  switch (f.getLabel()) {
                    case "update" -> DateRangeFilter.DateField.UPDATE;
                    case "create" -> DateRangeFilter.DateField.CREATE;
                    case "release" -> DateRangeFilter.DateField.RELEASE;
                    case "submitted" -> DateRangeFilter.DateField.SUBMITTED;
                    default ->
                        throw new IllegalArgumentException("Unknown date field " + f.getLabel());
                  };
              grpcFilters.add(
                  Filter.newBuilder()
                      .setDateRange(
                          DateRangeFilter.newBuilder()
                              .setField(dateField)
                              .setFrom(dateRange.getFrom().toString())
                              .setTo(dateRange.getUntil().toString()))
                      .build());
            });
  }

  private static void getAttributeSearchFilter(
      uk.ac.ebi.biosamples.core.model.filter.AttributeFilter filter,
      Map<String, Filter.Builder> filterMap) {
    Filter.Builder fb =
        filterMap.getOrDefault(
            filter.getLabel(),
            Filter.newBuilder()
                .setAttribute(AttributeFilter.newBuilder().setField(filter.getLabel())));
    filter.getContent().ifPresent(attribute -> fb.getAttributeBuilder().addValues(attribute));
    filterMap.putIfAbsent(filter.getLabel(), fb);
  }

  private static Filter getPrivateSearchFilter(String webinId) {
    PublicFilter.Builder publicFilterBuilder = PublicFilter.newBuilder();
    if (StringUtils.hasText(webinId)) {
      publicFilterBuilder.setWebinId(webinId);
    }
    return Filter.newBuilder().setPublic(publicFilterBuilder.build()).build();
  }
}
