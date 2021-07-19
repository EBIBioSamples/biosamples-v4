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
package uk.ac.ebi.biosamples.model;

import static junit.framework.TestCase.assertTrue;

import java.time.Instant;
import java.util.*;
import org.junit.Test;

public class SampleTaxIdTest {

  @Test
  public void given_single_ontologyTerm_return_taxId() {
    String olsValue = "http://purl.obolibrary.org/obo/NCBITaxon_10116";
    Attribute attribute =
        Attribute.build("Organism", "", null, Collections.singletonList(olsValue), null);
    Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(10116 == sample.getTaxId());
  }

  @Test
  public void given_single_ontologyTerm_return_taxId_with_lowercase_organism() {
    String olsValue = "http://purl.obolibrary.org/obo/NCBITaxon_9685";
    Attribute attribute =
        Attribute.build("organism", "Felis catu", null, Collections.singletonList(olsValue), null);
    Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(9685 == sample.getTaxId());
  }

  @Test
  public void given_an_Organism_with_multiple_entries() {
    List<String> olsValues =
        Arrays.asList(
            "http://purl.obolibrary.org/obo/NCBITaxon_10116",
            "http://purl.obolibrary.org/obo/NCBITaxon_9685");
    Attribute attribute = Attribute.build("Organism", "Felis catu", null, olsValues, null);
    Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(10116 == sample.getTaxId());
  }

  @Test
  public void given_multiple_Organisms() {
    String olsValue1 = "http://purl.obolibrary.org/obo/NCBITaxon_10116";
    String olsValue2 = "http://purl.obolibrary.org/obo/NCBITaxon_9685";
    Attribute attribute1 = Attribute.build("Organism", "", olsValue1, null);
    Attribute attribute2 = Attribute.build("Organism", "", olsValue2, null);
    Sample sample = generateTestSample(Arrays.asList(attribute1, attribute2));
    assertTrue(10116 == sample.getTaxId());
  }

  @Test
  public void given_single_ontologyTerm_return_taxId_with_empty_iri() {
    String olsValue = "";
    Attribute attribute =
        Attribute.build("Organism", "", null, Collections.singletonList(olsValue), null);
    Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(null == sample.getTaxId());
  }

  @Test
  public void given_9606_ontologyTerm_return_taxId() {
    String value = "9606";
    Attribute attribute =
        Attribute.build("Organism", "", null, Collections.singletonList(value), null);
    Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(9606 == sample.getTaxId());
  }

  @Test
  public void given_no_ontologyTerm_return_unknown_taxId() {
    Attribute attribute = Attribute.build("Organism", "s", null, Collections.EMPTY_LIST, null);
    Sample sample = generateTestSample(Collections.singletonList(attribute));
    assertTrue(null == sample.getTaxId());
  }

  private Sample generateTestSample(List<Attribute> attributes) {
    Set<Attribute> attributeSet = new HashSet<>();
    for (Attribute attribute : attributes) {
      attributeSet.add(attribute);
    }
    return Sample.build(
        "",
        "",
        "",
        "",
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
