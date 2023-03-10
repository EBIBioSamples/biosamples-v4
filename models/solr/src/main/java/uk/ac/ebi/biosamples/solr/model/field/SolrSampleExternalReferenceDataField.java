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
import uk.ac.ebi.biosamples.model.facet.ExternalReferenceDataFacet;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.filter.ExternalReferenceDataFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.model.strategy.RegularFacetFetchStrategy;

@Component
public class SolrSampleExternalReferenceDataField extends SolrSampleField {

  public SolrSampleExternalReferenceDataField() {
    super();
  }

  public SolrSampleExternalReferenceDataField(final String readableLabel) {
    super(readableLabel);
  }

  /**
   * All subclasses should implement this constructor.
   *
   * @param readableLabel
   * @param solrDocumentLabel
   */
  public SolrSampleExternalReferenceDataField(
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
    return "_erd_ss";
  }

  @Override
  public boolean isEncodedField() {
    return true;
  }

  @Override
  public boolean isCompatibleWith(final Filter filter) {
    return filter instanceof ExternalReferenceDataFilter;
  }

  @Override
  public boolean canGenerateFacets() {
    return true;
  }

  @Override
  public Facet.Builder getFacetBuilder(final String facetLabel, final Long facetCount) {
    return new ExternalReferenceDataFacet.Builder(facetLabel, facetCount);
  }

  @Override
  public FacetFetchStrategy getFacetCollectionStrategy() {
    return new RegularFacetFetchStrategy();
  }

  @Override
  public Criteria getFilterCriteria(final Filter filter) {
    Criteria filterCriteria = null;

    if (filter instanceof ExternalReferenceDataFilter) {

      filterCriteria = new Criteria(getSolrLabel());

      final ExternalReferenceDataFilter extRefFilter = (ExternalReferenceDataFilter) filter;
      if (extRefFilter.getContent().isPresent()) {
        //                filterCriteria = filterCriteria.expression("/" +
        // extRefFilter.getContent().get() + "/");
        filterCriteria =
            filterCriteria.expression(String.format("\"%s\"", extRefFilter.getContent().get()));
      } else {
        filterCriteria = filterCriteria.isNotNull();
      }
    }

    return filterCriteria;
  }
}
