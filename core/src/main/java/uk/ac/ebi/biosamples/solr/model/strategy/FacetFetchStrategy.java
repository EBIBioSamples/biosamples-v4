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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.FacetQuery;
import uk.ac.ebi.biosamples.core.model.facet.Facet;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

public interface FacetFetchStrategy {

  /**
   * The strategy uses the results from the facet query to return a list of optional facet
   *
   * @param sampleRepository the repository that will be used to retrieve the facets
   * @param query the FacetQuery to retrieve the facet
   * @param facetFieldCountEntries the facet fields/count on which the facet will be calculated
   * @param pageable a page information
   * @return a list of optional facets
   */
  public List<Optional<Facet>> fetchFacetsUsing(
      SolrSampleRepository sampleRepository,
      FacetQuery query,
      List<Map.Entry<SolrSampleField, Long>> facetFieldCountEntries,
      Pageable pageable);

  public List<Optional<Facet>> fetchFacetsUsing(
      SolrSampleRepository sampleRepository,
      FacetQuery query,
      List<Map.Entry<SolrSampleField, Long>> facetFieldCountEntries,
      List<Map.Entry<SolrSampleField, Long>> rangeFieldCountEntries,
      Pageable pageable);
}
