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
package uk.ac.ebi.biosamples.solr.model.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.FacetQueryEntry;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountEntry;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountListContent;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

public class RegularFacetFetchStrategy implements FacetFetchStrategy {

  private Logger log = LoggerFactory.getLogger(getClass());

  /*

  */
  //  @Override
  public List<Optional<Facet>> fetchFacetsUsing(
      SolrSampleRepository solrSampleRepository,
      FacetQuery query,
      List<Entry<SolrSampleField, Long>> facetFieldCountEntries,
      Pageable facetPageable) {

    List<String> facetFieldNames =
        facetFieldCountEntries.stream()
            .map(Entry::getKey)
            .map(SolrSampleField::getSolrLabel)
            .collect(Collectors.toList());

    FacetPage<?> facetPage = solrSampleRepository.getFacets(query, facetFieldNames, facetPageable);

    List<Optional<Facet>> facetResults = new ArrayList<>();
    for (Field field : facetPage.getFacetFields()) {

      // Get the field info associated with returned field from solr
      Optional<Entry<SolrSampleField, Long>> optionalFieldInfo =
          facetFieldCountEntries.stream()
              .filter(entry -> entry.getKey().getSolrLabel().equals(field.getName()))
              .findFirst();

      if (!optionalFieldInfo.isPresent()) {
        throw new RuntimeException(
            "Unexpected field returned when getting facets for " + facetFieldCountEntries);
      }

      Entry<SolrSampleField, Long> fieldCountEntry = optionalFieldInfo.get();

      // Create the list of facet value-count for the returned field
      List<LabelCountEntry> listFacetContent = new ArrayList<>();
      for (FacetFieldEntry ffe : facetPage.getFacetResultPage(field)) {
        log.trace(
            "Adding "
                + fieldCountEntry.getKey().getReadableLabel()
                + " : "
                + ffe.getValue()
                + " with count "
                + ffe.getValueCount());
        listFacetContent.add(LabelCountEntry.build(ffe.getValue(), ffe.getValueCount()));
      }

      // Build the facet
      SolrSampleField solrSampleField = fieldCountEntry.getKey();
      if (solrSampleField.canGenerateFacets()) {
        Facet facet =
            solrSampleField
                .getFacetBuilder(solrSampleField.getReadableLabel(), fieldCountEntry.getValue())
                .withContent(new LabelCountListContent(listFacetContent))
                .build();
        facetResults.add(Optional.of(facet));
      }
      //            Optional<FacetType> associatedFacetType =
      // solrSampleField.getSolrFieldType().getFacetFilterFieldType().getFacetType();
      //            if(associatedFacetType.isPresent()) {
      //                FacetType facetType = associatedFacetType.get();
      //                String facetLabel = solrSampleField.getReadableLabel();
      //                Long facetCount = fieldCountEntry.getValue();
      //                Facet facet = facetType
      //                        .getBuilderForLabelAndCount(facetLabel, facetCount)
      //                        .withContent(new LabelCountListContent(listFacetContent))
      //                        .build();
      //                facetResults.add(Optional.of(facet));
      //            }
    }

    return facetResults;
  }

  //  @Override
  public List<Optional<Facet>> fetchFacetsUsing(
      SolrSampleRepository solrSampleRepository,
      FacetQuery query,
      List<Entry<SolrSampleField, Long>> facetFieldCountEntries,
      List<Map.Entry<SolrSampleField, Long>> rangeFieldCountEntries,
      Pageable facetPageable) {

    List<String> facetFieldNames =
        facetFieldCountEntries.stream()
            .map(Entry::getKey)
            .map(SolrSampleField::getSolrLabel)
            .collect(Collectors.toList());
    List<String> rangeFacetFieldNames =
        rangeFieldCountEntries.stream()
            .map(Entry::getKey)
            .map(SolrSampleField::getSolrLabel)
            .collect(Collectors.toList());

    Map<String, SolrSampleField> fieldMap =
        facetFieldCountEntries.stream()
            .map(Entry::getKey)
            .collect(Collectors.toMap(SolrSampleField::getSolrLabel, s -> s));
    rangeFieldCountEntries.stream()
        .map(Entry::getKey)
        .forEach(s -> fieldMap.put(s.getSolrLabel(), s));

    FacetPage<?> facetPage =
        solrSampleRepository.getFacets(query, facetFieldNames, rangeFacetFieldNames, facetPageable);

    List<Optional<Facet>> facetResults = new ArrayList<>();
    for (FacetQueryEntry q : facetPage.getFacetQueryResult().getContent()) {
      long fieldCount = q.getValueCount();
      if (fieldCount > 0) {
        String fieldName = q.getValue().split(":")[0];
        //      String readableFieldName = SolrFieldService.decodeFieldName(fieldName);
        SolrSampleField solrSampleField = fieldMap.get(fieldName);

        List<LabelCountEntry> listFacetContent = new ArrayList<>();
        for (FacetFieldEntry ffe : facetPage.getFacetResultPage(fieldName)) {
          listFacetContent.add(LabelCountEntry.build(ffe.getValue(), ffe.getValueCount()));
        }

        // todo only add if not empty or remove
        //        if (facetPage.getRangeFacetResultPage(fieldName).getTotalElements() < 1) {
        //          break;
        //        }
        for (FacetFieldEntry ffe : facetPage.getRangeFacetResultPage(fieldName)) {
          listFacetContent.add(LabelCountEntry.build(ffe.getValue(), ffe.getValueCount()));
        }

        if (!listFacetContent.isEmpty()) {
          Facet facet =
              solrSampleField
                  .getFacetBuilder(solrSampleField.getReadableLabel(), fieldCount)
                  .withContent(new LabelCountListContent(listFacetContent))
                  .build();
          facetResults.add(Optional.of(facet));
        }
      }
    }
    return facetResults;
  }
}
