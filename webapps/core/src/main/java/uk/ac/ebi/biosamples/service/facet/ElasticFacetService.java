package uk.ac.ebi.biosamples.service.facet;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.ac.ebi.biosamples.core.model.facet.*;
import uk.ac.ebi.biosamples.core.model.facet.Facet;
import uk.ac.ebi.biosamples.core.model.facet.content.LabelCountEntry;
import uk.ac.ebi.biosamples.core.model.facet.content.LabelCountListContent;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.search.grpc.*;
import uk.ac.ebi.biosamples.service.search.GrpcFilterUtils;
import uk.ac.ebi.biosamples.service.search.SearchAfterPage;
import uk.ac.ebi.biosamples.service.search.SearchService;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

import java.util.*;

@Service("elasticFacetService")
@RequiredArgsConstructor
@Slf4j
public class ElasticFacetService implements FacetService {

  @Override
  @Timed("biosamples.facet.page.elastic")
  public List<Facet> getFacets(String searchTerm, Set<Filter> filters, String webinId,
                               Pageable facetFieldPageInfo, Pageable facetValuesPageInfo,
                               String facetField, List<String> facetFields) {

    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090).usePlaintext().build();
    SearchGrpc.SearchBlockingStub stub = SearchGrpc.newBlockingStub(channel);
    FacetResponse response;
    try {
      FacetRequest.Builder builder = FacetRequest.newBuilder();
      if (StringUtils.hasText(searchTerm)) {
        builder.setText(searchTerm);
      }

      builder.addAllFilters(GrpcFilterUtils.getSearchFilters(filters, webinId));

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

  public static List<Facet> convertToFacets(List<uk.ac.ebi.biosamples.search.grpc.Facet> grpcFacets) {
    return grpcFacets.stream().map(ElasticFacetService::convertFacet).toList();
  }

  static Facet convertFacet(uk.ac.ebi.biosamples.search.grpc.Facet grpcFacet) {
    Facet.Builder facetBuilder = switch (grpcFacet.getType()) {
      case "attr" -> new AttributeFacet.Builder(grpcFacet.getField(), grpcFacet.getCount())
          .withContent(convertToLabelCounts(grpcFacet.getBucketsMap()));
      case "dt" -> new DateRangeFacet.Builder(grpcFacet.getField(), grpcFacet.getCount())
          .withContent(convertToLabelCounts(grpcFacet.getBucketsMap()));
      case "rel" -> new RelationFacet.Builder(grpcFacet.getField(), grpcFacet.getCount())
          .withContent(convertToLabelCounts(grpcFacet.getBucketsMap()));
      case "extd" -> new ExternalReferenceDataFacet.Builder(grpcFacet.getField(), grpcFacet.getCount())
          .withContent(convertToLabelCounts(grpcFacet.getBucketsMap()));
//      case "sdata" -> new DateRangeFacet.Builder(grpcFacet.getField(), grpcFacet.getCount());
      default -> new AttributeFacet.Builder(grpcFacet.getField(), grpcFacet.getCount())
          .withContent(convertToLabelCounts(grpcFacet.getBucketsMap()));
    };
    return facetBuilder.build();
  }

  static LabelCountListContent convertToLabelCounts(Map<String, Long> labelCounts) {
    List<LabelCountEntry> labelCountEntries = labelCounts.entrySet().stream()
        .map(e -> LabelCountEntry.build(e.getKey(), e.getValue())).toList();
    return new LabelCountListContent(labelCountEntries);
  }
}
