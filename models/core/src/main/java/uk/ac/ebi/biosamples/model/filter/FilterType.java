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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FilterType {
  ATTRIBUTE_FILTER("attr", AttributeFilter.Builder.class),
  NAME_FILTER("name", NameFilter.Builder.class),
  RELATION_FILER("rel", RelationFilter.Builder.class),
  INVERSE_RELATION_FILTER("rrel", InverseRelationFilter.Builder.class),
  DOMAIN_FILTER("dom", DomainFilter.Builder.class),
  DATE_FILTER("dt", DateRangeFilter.DateRangeFilterBuilder.class),
  EXTERNAL_REFERENCE_DATA_FILTER("extd", ExternalReferenceDataFilter.Builder.class),
  ACCESSION_FILTER("acc", AccessionFilter.Builder.class);

  private static List<FilterType> filterTypesByLength = new ArrayList<>();

  static {
    filterTypesByLength =
        Stream.of(values())
            .sorted(
                Comparator.comparingInt((FilterType f) -> f.getSerialization().length())
                    .reversed()
                    .thenComparing((FilterType::getSerialization)))
            .collect(Collectors.toList());
  }

  String serialization;
  Class<? extends Filter.Builder> associatedBuilder;

  FilterType(String serialization, Class<? extends Filter.Builder> associatedBuilder) {
    this.serialization = serialization;
    this.associatedBuilder = associatedBuilder;
  }

  public String getSerialization() {
    return this.serialization;
  }

  public Filter.Builder getBuilderForLabel(String label) {
    try {
      return this.associatedBuilder.getConstructor(String.class).newInstance(label);
    } catch (NoSuchMethodException
        | IllegalAccessException
        | InstantiationException
        | InvocationTargetException e) {
      throw new RuntimeException("FilterType " + this + " does not provide a proper builder");
    }
  }

  public static FilterType ofFilterString(String filterString) {
    for (FilterType type : filterTypesByLength) {
      if (filterString.startsWith(type.getSerialization())) {
        return type;
      }
    }
    throw new IllegalArgumentException("Cannot infer filter type from string " + filterString);
  }
}
