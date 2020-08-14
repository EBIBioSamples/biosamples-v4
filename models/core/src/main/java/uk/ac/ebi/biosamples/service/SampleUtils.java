/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class SampleUtils {

  public String getAccessionPattern() {
    return "SAM[END][AG]?[0-9]+";
  }

  /**
   * Get an optional list of attributes matching the provided type without case sensitivity
   *
   * @param sample the sample to check
   * @param attributeType the type to use as filter
   * @return an optional list of attributes
   */
  public Optional<List<Attribute>> getAttributesWithType(Sample sample, String attributeType) {
    return getAttributesWithType(sample, attributeType, false);
  }

  /**
   * Get an optional list of attributes matching the provided type
   *
   * @param sample the sample to check
   * @param attributeType the type to use as filter
   * @param useCaseSensitivity if the match should be case sensitive
   * @return an optional list of attributes
   */
  public Optional<List<Attribute>> getAttributesWithType(
      Sample sample, String attributeType, boolean useCaseSensitivity) {
    return Optional.ofNullable(
        sample.getCharacteristics().stream()
            .filter(
                attr ->
                    useCaseSensitivity
                        ? attr.getType().equals(attributeType)
                        : attr.getType().equalsIgnoreCase(attributeType))
            .collect(Collectors.toList()));
  }

  /**
   * Get an optional list of attributes matching the provided type regular expression
   *
   * @param sample the sample to check
   * @param typeRegex the regular expression to match the type
   * @return an optional list of attributes
   */
  public Optional<List<Attribute>> getAttributesWithTypeMatching(Sample sample, String typeRegex) {
    return Optional.ofNullable(
        sample.getCharacteristics().stream()
            .filter(attr -> attr.getType().matches(typeRegex))
            .collect(Collectors.toList()));
  }

  /**
   * Get an optional list of attributes matching the provided value
   *
   * @param sample the sample to check
   * @param attributeValue the attribute to use as filter
   * @param useCaseSensitivity if the match should be case sensitive
   * @return an optional list of attributes
   */
  public Optional<List<Attribute>> getAttributesWithValue(
      Sample sample, String attributeValue, boolean useCaseSensitivity) {
    return Optional.ofNullable(
        sample.getCharacteristics().stream()
            .filter(
                attr ->
                    useCaseSensitivity
                        ? attr.getValue().equals(attributeValue)
                        : attr.getValue().equalsIgnoreCase(attributeValue))
            .collect(Collectors.toList()));
  }

  /**
   * Get an optional list of attributes matching the provided value without case sensitivity
   *
   * @param sample the sample to check
   * @param attributeValue the attribute to use as filter
   * @return an optional list of attributes
   */
  public Optional<List<Attribute>> getAttributesWithValue(Sample sample, String attributeValue) {
    return getAttributesWithValue(sample, attributeValue, false);
  }

  /**
   * Get an optional list of attributes matching the provided value regular expression
   *
   * @param sample the sample to check
   * @param valueRegex the attribute value regular expression
   * @return an optional list of attributes
   */
  public Optional<List<Attribute>> getAttributesWithValueMatching(
      Sample sample, String valueRegex) {
    return Optional.ofNullable(
        sample.getCharacteristics().stream()
            .filter(attr -> attr.getValue().matches(valueRegex))
            .collect(Collectors.toList()));
  }
}
