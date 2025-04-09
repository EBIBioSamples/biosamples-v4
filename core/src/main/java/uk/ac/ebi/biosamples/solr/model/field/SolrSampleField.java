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
import uk.ac.ebi.biosamples.core.model.facet.Facet;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;
import uk.ac.ebi.biosamples.solr.service.SolrFieldService;

public abstract class SolrSampleField implements FilterCriteriaBuilder {

  private String readableLabel;
  private String solrDocumentLabel;

  /** Constructor meant to be used only for reflection purposes */
  protected SolrSampleField() {
    readableLabel = null;
    solrDocumentLabel = null;
  }

  protected SolrSampleField(final String readableLabel) {
    this.readableLabel = readableLabel;
    if (isEncodedField()) {
      solrDocumentLabel = SolrFieldService.encodeFieldName(readableLabel) + getSolrFieldSuffix();
    } else {
      solrDocumentLabel = readableLabel + getSolrFieldSuffix();
    }
  }

  /**
   * All subclasses should implement this constructor.
   *
   * @param readableLabel
   * @param solrDocumentLabel
   */
  protected SolrSampleField(final String readableLabel, final String solrDocumentLabel) {
    this.readableLabel = readableLabel;
    this.solrDocumentLabel = solrDocumentLabel;
  }

  /**
   * Check if the provided string matches the field regularExpression
   *
   * @param fieldName string to check against the field pattern
   * @return
   */
  public boolean matches(final String fieldName) {
    return getSolrFieldPattern().matcher(fieldName).find();
  }

  /**
   * Return the regular expression pattern that matches the field in a Solr document. The pattern
   * contains usually two groups: fieldname and fieldsuffix Usually is structured this way
   *
   * <pre>/^(?<fieldname>pattern>(?<fieldsuffix>suffix)$</pre>
   *
   * @return the Pattern to match the field in a solr document
   */
  public abstract Pattern getSolrFieldPattern();

  /**
   * Return the solr field suffix
   *
   * @return
   */
  public abstract String getSolrFieldSuffix();

  /**
   * Return if the field in the solr document is encoded or not
   *
   * @return
   */
  public abstract boolean isEncodedField();

  /**
   * Return if the solr field is compatible with a specific Filter class TODO: if in future we want
   * to compose filters (NOT, REGEX, etc.) This should look inside the filter itself
   *
   * @param filter the filter to test for compatibility
   * @return if the field is compatible with the filter class
   */
  public abstract boolean isCompatibleWith(Filter filter);

  /**
   * Return if the field can be used to generate facets
   *
   * @return
   */
  public boolean canGenerateFacets() {
    return false;
  }

  /**
   * Return an optional builder for the facet corresponding to the field
   *
   * @param facetLabel
   * @param facetCount
   * @return
   */
  public Facet.Builder getFacetBuilder(final String facetLabel, final Long facetCount) {
    return null;
  }

  /**
   * For each field a specific strategy to get the facet content need to be implemented Facet
   * content retrieve will be delegated to the facet fetch strategy
   *
   * @return a facet fetch strategy
   */
  public abstract FacetFetchStrategy getFacetCollectionStrategy();

  /** @return the readable label of the field */
  public String getReadableLabel() {
    return readableLabel;
  }

  /**
   * @return the document field, which could be encoded or not encoded based on the SolrFieldType
   */
  public String getSolrLabel() {
    return solrDocumentLabel;
  }

  public SolrSampleField setReadableLabel(final String readableLabel) {
    this.readableLabel = readableLabel;
    return this;
  }

  public SolrSampleField setSolrLabel(final String solrDocumentLabel) {
    this.solrDocumentLabel = solrDocumentLabel;
    return this;
  }
}
