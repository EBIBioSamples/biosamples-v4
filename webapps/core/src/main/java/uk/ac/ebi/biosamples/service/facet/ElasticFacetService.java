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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.micrometer.core.annotation.Timed;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.core.model.facet.*;
import uk.ac.ebi.biosamples.core.model.facet.Facet;
import uk.ac.ebi.biosamples.core.model.facet.content.LabelCountEntry;
import uk.ac.ebi.biosamples.core.model.facet.content.LabelCountListContent;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.search.grpc.*;
import uk.ac.ebi.biosamples.service.search.SearchFilterMapper;

@Service("elasticFacetService")
@RequiredArgsConstructor
@Slf4j
public class ElasticFacetService implements FacetService {
  private final BioSamplesProperties bioSamplesProperties;

  @Override
  @Timed("biosamples.facet.page.elastic")
  public List<Facet> getFacets(
      String searchTerm,
      Set<Filter> filters,
      String webinId,
      Pageable facetFieldPageInfo,
      Pageable facetValuesPageInfo,
      String facetField,
      List<String> facetFields) {

    ManagedChannel channel =
        ManagedChannelBuilder.forAddress(
                bioSamplesProperties.getBiosamplesSearchHost(),
                bioSamplesProperties.getBiosamplesSearchPort())
            .usePlaintext()
            .build();
    SearchGrpc.SearchBlockingStub stub = SearchGrpc.newBlockingStub(channel);
    FacetResponse response;
    try {
      FacetRequest.Builder builder = FacetRequest.newBuilder();
      if (StringUtils.hasText(searchTerm)) {
        builder.setText(searchTerm);
      }
      builder.addAllFilters(SearchFilterMapper.getSearchFilters(filters, webinId));
      if (facetFields != null) {
        builder.addAllFacets(facetFields);
      }
      builder.setSize(facetFieldPageInfo.getPageSize());

      response = stub.getFacets(builder.build());
    } catch (StatusRuntimeException e) {
      log.error("Failed to fetch samples from remote server", e);
      throw new RuntimeException("Failed to fetch samples from remote server", e);
    } finally {
      channel.shutdown();
    }

    List<uk.ac.ebi.biosamples.search.grpc.Facet> facets = response.getFacetsList();
    return convertToFacets(facets);
  }

  public static List<Facet> convertToFacets(
      List<uk.ac.ebi.biosamples.search.grpc.Facet> grpcFacets) {
    return grpcFacets.stream().map(ElasticFacetService::convertFacet).toList();
  }

  static Facet convertFacet(uk.ac.ebi.biosamples.search.grpc.Facet grpcFacet) {
    Facet.Builder facetBuilder =
        switch (grpcFacet.getType()) {
          case "attr" ->
              new AttributeFacet.Builder(grpcFacet.getField(), grpcFacet.getCount())
                  .withContent(convertToLabelCounts(grpcFacet.getBucketsMap()));
          case "dt" ->
              new DateRangeFacet.Builder(grpcFacet.getField(), grpcFacet.getCount())
                  .withContent(convertToLabelCounts(grpcFacet.getBucketsMap()));
          case "rel" ->
              new RelationFacet.Builder(grpcFacet.getField(), grpcFacet.getCount())
                  .withContent(convertToLabelCounts(grpcFacet.getBucketsMap()));
          case "extd" ->
              new ExternalReferenceDataFacet.Builder(grpcFacet.getField(), grpcFacet.getCount())
                  .withContent(convertToLabelCounts(grpcFacet.getBucketsMap()));
          //      case "sdata" -> new DateRangeFacet.Builder(grpcFacet.getField(),
          // grpcFacet.getCount());
          default ->
              new AttributeFacet.Builder(grpcFacet.getField(), grpcFacet.getCount())
                  .withContent(convertToLabelCounts(grpcFacet.getBucketsMap()));
        };
    return facetBuilder.build();
  }

  static LabelCountListContent convertToLabelCounts(Map<String, Long> labelCounts) {
    List<LabelCountEntry> labelCountEntries =
        labelCounts.entrySet().stream()
            .map(e -> LabelCountEntry.build(e.getKey(), e.getValue()))
            .toList();
    return new LabelCountListContent(labelCountEntries);
  }
}
