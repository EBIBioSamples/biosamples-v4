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
package uk.ac.ebi.biosamples.solr.model.field;

import java.util.regex.Pattern;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.core.model.facet.Facet;
import uk.ac.ebi.biosamples.core.model.facet.InverseRelationFacet;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.core.model.filter.InverseRelationFilter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

@Component
public class SolrSampleInverseRelationField extends SolrSampleField {

  public SolrSampleInverseRelationField() {
    super();
  }

  public SolrSampleInverseRelationField(final String readableLabel) {
    super(readableLabel);
  }

  /**
   * All subclasses should implement this constructor
   *
   * @param readableLabel
   * @param solrDocumentLabel
   */
  public SolrSampleInverseRelationField(
      final String readableLabel, final String solrDocumentLabel) {
    super(readableLabel, solrDocumentLabel);
  }

  @Override
  public Pattern getSolrFieldPattern() {
    return Pattern.compile(
        "^(?<fieldname>[A-Z0-9_]+)(?<fieldsuffix>" + getSolrFieldSuffix() + ")$");
  }

  @Override
  public String getSolrFieldSuffix() {
    return "_ir_ss";
  }

  @Override
  public boolean isEncodedField() {
    return true;
  }

  @Override
  public boolean isCompatibleWith(final Filter filter) {
    return filter instanceof InverseRelationFilter;
  }

  @Override
  public boolean canGenerateFacets() {
    return true;
  }

  @Override
  public Facet.Builder getFacetBuilder(final String facetLabel, final Long facetCount) {
    return new InverseRelationFacet.Builder(facetLabel, facetCount);
  }

  @Override
  public FacetFetchStrategy getFacetCollectionStrategy() {
    return new RegularFacetFetchStrategy();
  }

  @Override
  public Criteria getFilterCriteria(final Filter filter) {
    Criteria filterCriteria = null;

    if (filter instanceof InverseRelationFilter) {

      filterCriteria = new Criteria(getSolrLabel());

      final InverseRelationFilter inverseRelationFilter = (InverseRelationFilter) filter;
      if (inverseRelationFilter.getContent().isPresent()) {
        //                filterCriteria = filterCriteria.expression("/" +
        // inverseRelationFilter.getContent().get() + "/");
        filterCriteria =
            filterCriteria.expression(
                String.format("\"%s\"", inverseRelationFilter.getContent().get()));
      } else {
        filterCriteria = filterCriteria.isNotNull();
      }
    }

    return filterCriteria;
  }
}
