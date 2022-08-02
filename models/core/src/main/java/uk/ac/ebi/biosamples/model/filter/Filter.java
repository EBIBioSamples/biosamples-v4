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
package uk.ac.ebi.biosamples.model.filter;

import java.util.Optional;
import uk.ac.ebi.biosamples.model.facet.FacetType;

public interface Filter {

  public FilterType getType();

  /**
   * The label the filter is targeting
   *
   * @return
   */
  public String getLabel();

  /**
   * Return the optional content of the filter. The content is optional because can be used also to
   * filter for samples having a specific characteristic
   *
   * @return filter specific value, if available
   */
  public Optional<?> getContent();

  /**
   * Generate the serialized version of the filter usable through the web interface
   *
   * @return string representing the filter value
   */
  public String getSerialization();

  /**
   * Get the facet associated to the filter, if any is available
   *
   * @return optional facet type
   */
  public FacetType getAssociatedFacetType();

  public interface Builder {
    public Filter build();

    /**
     * Create a builder starting from a filter serialization
     *
     * @param filterSerialized string representing a filter
     * @return a Builder to compose the filter
     */
    public Filter.Builder parseContent(String filterSerialized);
  }
}
