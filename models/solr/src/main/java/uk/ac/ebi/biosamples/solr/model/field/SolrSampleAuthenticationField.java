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
import uk.ac.ebi.biosamples.model.filter.AuthenticationFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.strategy.FacetFetchStrategy;

@Component
public class SolrSampleAuthenticationField extends SolrSampleField {
  public SolrSampleAuthenticationField() {
    super();
  }

  public SolrSampleAuthenticationField(final String readableLabel) {
    super(readableLabel);
  }

  /**
   * All subclasses should implement this constructor
   *
   * @param readableLabel
   * @param solrDocumentLabel
   */
  public SolrSampleAuthenticationField(final String readableLabel, final String solrDocumentLabel) {
    super(readableLabel, solrDocumentLabel);
  }

  @Override
  public Pattern getSolrFieldPattern() {
    return Pattern.compile(
        "^(?<fieldname>domain|webinId)(?<fieldsuffix>" + getSolrFieldSuffix() + ")$");
  }

  @Override
  public String getSolrFieldSuffix() {
    return "_s";
  }

  @Override
  public boolean isEncodedField() {
    return false;
  }

  @Override
  public boolean isCompatibleWith(final Filter filter) {
    return filter instanceof AuthenticationFilter;
  }

  @Override
  public FacetFetchStrategy getFacetCollectionStrategy() {
    return null;
  }

  @Override
  public Criteria getFilterCriteria(final Filter filter) {
    Criteria filterCriteria = null;

    if (filter instanceof AuthenticationFilter) {
      filterCriteria = new Criteria(getSolrLabel());

      final AuthenticationFilter authenticationFilter = (AuthenticationFilter) filter;

      if (authenticationFilter.getContent().isPresent()) {
        filterCriteria =
            filterCriteria.expression(
                String.format("\"%s\"", authenticationFilter.getContent().get()));
      } else {
        filterCriteria = filterCriteria.isNotNull();
      }
    }

    return filterCriteria;
  }
}