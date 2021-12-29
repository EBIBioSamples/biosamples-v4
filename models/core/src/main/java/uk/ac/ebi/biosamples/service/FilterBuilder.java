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
import uk.ac.ebi.biosamples.model.filter.AccessionFilter;
import uk.ac.ebi.biosamples.model.filter.AttributeFilter;
import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.DomainFilter;
import uk.ac.ebi.biosamples.model.filter.ExternalReferenceDataFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.filter.FilterType;
import uk.ac.ebi.biosamples.model.filter.InverseRelationFilter;
import uk.ac.ebi.biosamples.model.filter.NameFilter;
import uk.ac.ebi.biosamples.model.filter.RelationFilter;

@Service
public class FilterBuilder {
  public AttributeFilter.Builder onAttribute(String label) {
    return new AttributeFilter.Builder(label);
  }

  public RelationFilter.Builder onRelation(String label) {
    return new RelationFilter.Builder(label);
  }

  public InverseRelationFilter.Builder onInverseRelation(String label) {
    return new InverseRelationFilter.Builder(label);
  }

  public DateRangeFilter.DateRangeFilterBuilder onDate(String fieldLabel) {
    return new DateRangeFilter.DateRangeFilterBuilder(fieldLabel);
  }

  public DateRangeFilter.DateRangeFilterBuilder onReleaseDate() {
    return new DateRangeFilter.DateRangeFilterBuilder("release");
  }

  public DateRangeFilter.DateRangeFilterBuilder onUpdateDate() {
    return new DateRangeFilter.DateRangeFilterBuilder("update");
  }

  public DomainFilter.Builder onDomain(String domain) {
    return new DomainFilter.Builder(domain);
  }

  public NameFilter.Builder onName(String name) {
    return new NameFilter.Builder(name);
  }

  public AccessionFilter.Builder onAccession(String accession) {
    return new AccessionFilter.Builder(accession);
  }

  public ExternalReferenceDataFilter.Builder onDataFromExternalReference(String extReference) {
    return new ExternalReferenceDataFilter.Builder(extReference);
  }

  public Filter buildFromString(String serializedFilter) {
    FilterType filterType = FilterType.ofFilterString(serializedFilter);
    /* if (filterType != FilterType.ACCESSION_FILTER) {
      serializedFilter = serializedFilter.toLowerCase();
    }*/
    List<String> filterParts = filterParts(serializedFilter);

    if (filterParts.size() > 2) {
      return filterType
          .getBuilderForLabel(filterParts.get(1))
          .parseContent(filterParts.get(2))
          .build();
    } else {
      return filterType.getBuilderForLabel(filterParts.get(1)).build();
    }
  }

  private List<String> filterParts(String filterLabelAndValue) {
    // TODO hack, need to be improved
    return Arrays.stream(filterLabelAndValue.split("(?<!\\\\):", 3))
        .map(s -> s.replace("\\:", ":"))
        .collect(Collectors.toList());
  }

  public static FilterBuilder create() {
    return new FilterBuilder();
  }
}
