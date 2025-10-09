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
package uk.ac.ebi.biosamples.core.model;

import static junit.framework.TestCase.assertTrue;

import java.time.Instant;
import java.util.*;
import org.junit.Test;

public class SampleTaxIdTest {

  @Test
  public void given_single_ontologyTerm_return_taxId() {
    final String olsValue = "http://purl.obolibrary.org/obo/NCBITaxon_10116";
    final Attribute attribute =
        Attribute.build("Organism", "", null, Collections.singletonList(olsValue), null);
    final Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(10116 == sample.getTaxId());
  }

  @Test
  public void given_single_ontologyTerm_return_taxId_with_lowercase_organism() {
    final String olsValue = "http://purl.obolibrary.org/obo/NCBITaxon_9685";
    final Attribute attribute =
        Attribute.build("organism", "Felis catu", null, Collections.singletonList(olsValue), null);
    final Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(9685 == sample.getTaxId());
  }

  @Test
  public void given_an_Organism_with_multiple_entries() {
    final List<String> olsValues =
        Arrays.asList(
            "http://purl.obolibrary.org/obo/NCBITaxon_10116",
            "http://purl.obolibrary.org/obo/NCBITaxon_9685");
    final Attribute attribute = Attribute.build("Organism", "Felis catu", null, olsValues, null);
    final Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(10116 == sample.getTaxId());
  }

  @Test
  public void given_multiple_Organisms() {
    final String olsValue1 = "http://purl.obolibrary.org/obo/NCBITaxon_10116";
    final String olsValue2 = "http://purl.obolibrary.org/obo/NCBITaxon_9685";
    final Attribute attribute1 = Attribute.build("Organism", "", olsValue1, null);
    final Attribute attribute2 = Attribute.build("Organism", "", olsValue2, null);
    final Sample sample = generateTestSample(Arrays.asList(attribute1, attribute2));
    assertTrue(10116 == sample.getTaxId());
  }

  @Test
  public void given_single_ontologyTerm_return_taxId_with_empty_iri() {
    final String olsValue = "";
    final Attribute attribute =
        Attribute.build("Organism", "", null, Collections.singletonList(olsValue), null);
    final Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(null == sample.getTaxId());
  }

  @Test
  public void given_9606_ontologyTerm_return_taxId() {
    final String value = "9606";
    final Attribute attribute =
        Attribute.build("Organism", "", null, Collections.singletonList(value), null);
    final Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(9606 == sample.getTaxId());
  }

  @Test
  public void given_no_ontologyTerm_return_unknown_taxId() {
    final Attribute attribute =
        Attribute.build("Organism", "s", null, Collections.EMPTY_LIST, null);
    final Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(null == sample.getTaxId());
  }

  private Sample generateTestSample(final List<Attribute> attributes) {
    final Set<Attribute> attributeSet = new HashSet<>(attributes);

    return Sample.build(
        "",
        "",
        "",
        "",
        "",
        null,
        null,
        Instant.now(),
        Instant.now(),
        Instant.now(),
        Instant.now(),
        Instant.now(),
        attributeSet,
        Collections.emptySet(),
        Collections.emptySet());
  }
}
