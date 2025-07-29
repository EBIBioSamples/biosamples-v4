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
package uk.ac.ebi.biosamples.solr.service;

import com.google.common.io.BaseEncoding;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;

/**
 * SolrFieldService is the service that should be able to deal with all field matters - Encode and
 * decode of a field is the main reason behind it
 */
@Service
public class SolrFieldService {

  private final List<SolrSampleField> solrFieldList;

  @Autowired
  public SolrFieldService(final List<SolrSampleField> solrSampleFields) {
    solrFieldList = solrSampleFields;
  }

  public List<SolrSampleField> getSolrFieldList() {
    return solrFieldList;
  }

  public static String encodeFieldName(String field) {
    // solr only allows alphanumeric field types
    try {
      field = BaseEncoding.base32().encode(field.getBytes("UTF-8"));
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    // although its base32 encoded, that include = which solr doesn't allow
    field = field.replaceAll("=", "_");

    return field;
  }

  public static String decodeFieldName(final String encodedField) {
    // although its base32 encoded, that include = which solr doesn't allow
    String decodedField = encodedField.replace("_", "=");
    try {
      decodedField = new String(BaseEncoding.base32().decode(decodedField), "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return decodedField;
  }

  /**
   * Try to decode a field guessing the facet type
   *
   * @param encodedField encoded version of the field with the type suffix
   * @return the field name decoded
   */
  public SolrSampleField decodeField(final String encodedField) {

    final Optional<SolrSampleField> optionalType =
        solrFieldList.stream().filter(solrField -> solrField.matches(encodedField)).findFirst();
    if (optionalType.isPresent()) {
      final SolrSampleField fieldCandidate = optionalType.get();
      final Matcher m = fieldCandidate.getSolrFieldPattern().matcher(encodedField);
      if (m.find()) {
        String baseLabel = m.group("fieldname");

        if (fieldCandidate.isEncodedField()) {
          baseLabel = decodeFieldName(baseLabel);
        }
        try {

          return getNewFieldInstance(fieldCandidate.getClass(), baseLabel, encodedField);

        } catch (final NoSuchMethodException
            | IllegalAccessException
            | InvocationTargetException
            | InstantiationException e) {
          throw new RuntimeException(
              "An error occurred while instantiating creating a new instance of class "
                  + fieldCandidate.getClass());
        }
      }
    }

    throw new RuntimeException("Provide field " + encodedField + " is unknown");
  }

  public SolrSampleField getCompatibleField(final Filter filter) {

    final Optional<SolrSampleField> optionalType =
        solrFieldList.stream().filter(solrField -> solrField.isCompatibleWith(filter)).findFirst();
    if (optionalType.isPresent()) {
      final SolrSampleField fieldCandidate = optionalType.get();
      // TODO implement methods to extract suffix and generate also the encoded label

      try {

        return getNewFieldInstance(fieldCandidate.getClass(), filter.getLabel());

      } catch (final NoSuchMethodException
          | IllegalAccessException
          | InvocationTargetException
          | InstantiationException e) {
        throw new RuntimeException(
            "An error occurred while instantiating creating a new instance of class "
                + fieldCandidate.getClass());
      }
    }

    throw new RuntimeException("Provide filter " + filter + " is unknown");
  }

  public SolrSampleField getNewFieldInstance(
      final Class<? extends SolrSampleField> prototype,
      final String baseLabel,
      final String encodedLabel)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
          InstantiationException {
    return prototype
        .getConstructor(String.class, String.class)
        .newInstance(baseLabel, encodedLabel);
  }

  public SolrSampleField getNewFieldInstance(
      final Class<? extends SolrSampleField> prototype, final String baseLabel)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
          InstantiationException {
    return prototype.getConstructor(String.class).newInstance(baseLabel);
  }
}
