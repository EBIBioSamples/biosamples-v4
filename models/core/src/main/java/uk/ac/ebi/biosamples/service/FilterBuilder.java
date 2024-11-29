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
package uk.ac.ebi.biosamples.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.*;

@Service
public class FilterBuilder {
  public AttributeFilter.Builder onAttribute(final String label) {
    return new AttributeFilter.Builder(label);
  }

  public RelationFilter.Builder onRelation(final String label) {
    return new RelationFilter.Builder(label);
  }

  public InverseRelationFilter.Builder onInverseRelation(final String label) {
    return new InverseRelationFilter.Builder(label);
  }

  public DateRangeFilter.DateRangeFilterBuilder onDate(final String fieldLabel) {
    return new DateRangeFilter.DateRangeFilterBuilder(fieldLabel);
  }

  public DateRangeFilter.DateRangeFilterBuilder onReleaseDate() {
    return new DateRangeFilter.DateRangeFilterBuilder("release");
  }

  public DateRangeFilter.DateRangeFilterBuilder onUpdateDate() {
    return new DateRangeFilter.DateRangeFilterBuilder("update");
  }

  public AuthenticationFilter.Builder onAuthInfo(final String domain) {
    return new AuthenticationFilter.Builder(domain);
  }

  public NameFilter.Builder onName(final String name) {
    return new NameFilter.Builder(name);
  }

  public AccessionFilter.Builder onAccession(final String accession) {
    return new AccessionFilter.Builder(accession);
  }

  public ExternalReferenceDataFilter.Builder onDataFromExternalReference(
      final String extReference) {
    return new ExternalReferenceDataFilter.Builder(extReference);
  }

  public Filter buildFromString(final String serializedFilter) {
    final FilterType filterType = FilterType.ofFilterString(serializedFilter);
    final List<String> filterParts = filterParts(serializedFilter);

    if (filterParts.size() > 2) {
      return filterType
          .getBuilderForLabel(filterParts.get(1))
          .parseContent(filterParts.get(2))
          .build();
    } else {
      return filterType.getBuilderForLabel(filterParts.get(1)).build();
    }
  }

  private List<String> filterParts(final String filterLabelAndValue) {
    // TODO hack, need to be improved
    return Arrays.stream(filterLabelAndValue.split("(?<!\\\\):", 3))
        .map(s -> s.replace("\\:", ":"))
        .collect(Collectors.toList());
  }

  public static FilterBuilder create() {
    return new FilterBuilder();
  }
}
