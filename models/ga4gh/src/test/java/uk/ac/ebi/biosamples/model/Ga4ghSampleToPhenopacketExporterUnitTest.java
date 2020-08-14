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
package uk.ac.ebi.biosamples.model;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import org.json.JSONException;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenopackets.schema.v1.Phenopacket;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.core.io.ClassPathResource;
import uk.ac.ebi.biosamples.model.ga4gh.*;
import uk.ac.ebi.biosamples.service.*;

/** Unit testing of ga4gh to phenopacket exporter. */
public class Ga4ghSampleToPhenopacketExporterUnitTest {
  @Mock Ga4ghSample ga4ghSample;
  @Mock Ga4ghBiocharacteristics biocharacteristic;
  @Mock Ga4ghAttributes attributes;
  @Mock AttributeValue attributeValue;
  @Mock Ga4ghExternalIdentifier externalIdentifier;
  @Mock Ga4ghGeoLocation geoLocation;
  Ga4ghSampleToPhenopacketConverter exporter;
  @Mock SampleToGa4ghSampleConverter mapper;
  @Mock Ga4ghAge age;
  @Mock Ga4ghOntologyTerm term;
  @Mock OLSDataRetriever retriever;

  public Ga4ghSampleToPhenopacketExporterUnitTest() {
    setupMock();
    mapper =
        new SampleToGa4ghSampleConverter(
            new Ga4ghSample(new Ga4ghAttributes()),
            new GeoLocationDataHelper(),
            new HttpOlsUrlResolutionService());
    exporter = new Ga4ghSampleToPhenopacketConverter(mapper, retriever);
  }

  @Test
  public void testMapBiosampleToPhenopacketBasic() throws IOException, JSONException {
    Phenopacket phenoPacket = exporter.convert(ga4ghSample);
    String phenpacketJson = com.google.protobuf.util.JsonFormat.printer().print(phenoPacket);
    String expectedJson = getFileFromResources("/basicPhenopacketJson.json");
    JSONAssert.assertEquals(
        expectedJson,
        phenpacketJson,
        new CustomComparator(
            JSONCompareMode.LENIENT, new Customization("metaData.created", (o1, o2) -> true)));
  }

  @Test
  public void testIndividualMapping() throws IOException, JSONException {
    when(ga4ghSample.getIndividual_id()).thenReturn(null);
    Ga4ghBiocharacteristics sex = createBiocharacteristicByDescription("sex");
    Ga4ghBiocharacteristics organism = createBiocharacteristicByDescription("organism");
    SortedSet<Ga4ghBiocharacteristics> biocharacteristics = new TreeSet<>();
    biocharacteristics.add(sex);
    biocharacteristics.add(organism);
    when(ga4ghSample.getBio_characteristic()).thenReturn(biocharacteristics);
    Phenopacket phenoPacket = exporter.convert(ga4ghSample);
    String phenpacketJson = com.google.protobuf.util.JsonFormat.printer().print(phenoPacket);
    System.out.println(phenpacketJson);
    String expectedJson = getFileFromResources("/individualMappingTestPhenopacket.json");
    System.out.println("this\n");
    System.out.println(phenpacketJson);
    JSONAssert.assertEquals(
        expectedJson,
        phenpacketJson,
        new CustomComparator(
            JSONCompareMode.LENIENT, new Customization("metaData.created", (o1, o2) -> true)));
  }

  @Test
  public void testDiseaseMapping() throws IOException, JSONException {
    when(ga4ghSample.getIndividual_id()).thenReturn(null);
    Ga4ghBiocharacteristics disease = createBiocharacteristicByDescription("disease");
    SortedSet<Ga4ghBiocharacteristics> biocharacteristics = new TreeSet<>();
    biocharacteristics.add(disease);
    when(ga4ghSample.getBio_characteristic()).thenReturn(biocharacteristics);
    Phenopacket phenoPacket = exporter.convert(ga4ghSample);
    String phenpacketJson = com.google.protobuf.util.JsonFormat.printer().print(phenoPacket);
    String expectedJson = getFileFromResources("/diseaseTestPhenopacket.json");
    JSONAssert.assertEquals(
        expectedJson,
        phenpacketJson,
        new CustomComparator(
            JSONCompareMode.LENIENT, new Customization("metaData.created", (o1, o2) -> true)));
  }

  @Test
  public void testBiocharacteristics() throws IOException, JSONException {
    when(ga4ghSample.getIndividual_id()).thenReturn(null);
    Ga4ghBiocharacteristics phenotype = createBiocharacteristicByDescription("phenotype");
    Ga4ghBiocharacteristics phenotype1 = createBiocharacteristicByDescription("phenotype1");
    SortedSet<Ga4ghBiocharacteristics> biocharacteristics = new TreeSet<>();
    biocharacteristics.add(phenotype);
    biocharacteristics.add(phenotype1);
    when(ga4ghSample.getBio_characteristic()).thenReturn(biocharacteristics);
    Phenopacket phenoPacket = exporter.convert(ga4ghSample);
    String phenpacketJson = com.google.protobuf.util.JsonFormat.printer().print(phenoPacket);
    String expectedJson = getFileFromResources("/phenotypeTestPhenopacket.json");
    JSONAssert.assertEquals(
        expectedJson,
        phenpacketJson,
        new CustomComparator(
            JSONCompareMode.LENIENT, new Customization("metaData.created", (o1, o2) -> true)));
  }

  public Ga4ghBiocharacteristics createBiocharacteristicByDescription(String description) {
    Ga4ghBiocharacteristics biocharacteristics = new Ga4ghBiocharacteristics();
    biocharacteristics.setDescription(description);
    SortedSet<Ga4ghOntologyTerm> bioTerm = new TreeSet<>();
    bioTerm.add(term);
    biocharacteristics.setOntology_terms(bioTerm);
    return biocharacteristics;
  }

  public void setupMock() {
    MockitoAnnotations.initMocks(this);
    setupTerm();
    setupAge();
    setupBiocharacteristic();
    setupAttributeVlaue();
    setupAttributes();
    setupGeolocation();
    setupExternalIdentifier();
    setupOLSDataRetreiver();
    setupBiosample();
  }

  public void setupOLSDataRetreiver() {
    doNothing().when(retriever).readResourceInfoFromUrl(isA(String.class));
    doNothing().when(retriever).readOntologyJsonFromUrl(isA(String.class));
    when(retriever.getOntologyTermId()).thenReturn("term id");
    when(retriever.getOntologyTermLabel()).thenReturn("term label");
    when(retriever.getResourceId()).thenReturn("resource id");
    when(retriever.getResourceName()).thenReturn("resource name");
    when(retriever.getResourcePrefix()).thenReturn("resource prefix");
    when(retriever.getResourceUrl()).thenReturn("url");
    when(retriever.getResourceVersion()).thenReturn("version");
  }

  public void setupBiosample() {
    when(ga4ghSample.getName()).thenReturn("name");
    when(ga4ghSample.getId()).thenReturn("id");
    when(ga4ghSample.getDataset_id()).thenReturn("dataset_id");
    when(ga4ghSample.getDescription()).thenReturn("description");
    when(ga4ghSample.getIndividual_id()).thenReturn("individual-id");
    when(ga4ghSample.getReleasedDate()).thenReturn("2018-01-20T13:55:29.870Z");
    when(ga4ghSample.getUpdatedDate()).thenReturn("2018-01-20T13:55:29.870Z");
    when(ga4ghSample.getAttributes()).thenReturn(attributes);
    SortedSet<Ga4ghBiocharacteristics> biocharacteristics = new TreeSet<>();
    biocharacteristics.add(biocharacteristic);
    when(ga4ghSample.getBio_characteristic()).thenReturn(biocharacteristics);
    when(ga4ghSample.getLocation()).thenReturn(geoLocation);
    when(ga4ghSample.getIndividual_age_at_collection()).thenReturn(age);
    SortedSet<Ga4ghExternalIdentifier> externalIdentifiers = new TreeSet<>();
    externalIdentifiers.add(externalIdentifier);
    when(ga4ghSample.getExternal_identifiers()).thenReturn(externalIdentifiers);
  }

  public void setupAge() {
    when(age.getAge()).thenReturn("0");
    when(age.getAge_class()).thenReturn(term);
  }

  public void setupBiocharacteristic() {
    when(biocharacteristic.getDescription()).thenReturn("description");
    SortedSet<Ga4ghOntologyTerm> biocharacteristics = new TreeSet<>();
    biocharacteristics.add(term);
    when(biocharacteristic.getOntology_terms()).thenReturn(biocharacteristics);
    when(biocharacteristic.getScope()).thenReturn("scope");
  }

  public void setupTerm() {
    when(term.getTerm_label()).thenReturn("term label");
    when(term.getTerm_id()).thenReturn("term id");
  }

  public void setupExternalIdentifier() {
    when(externalIdentifier.getIdentifier()).thenReturn("identifier");
    when(externalIdentifier.getRelation()).thenReturn("relation");
  }

  public void setupGeolocation() {
    when(geoLocation.getLabel()).thenReturn("location");
    when(geoLocation.getAltitude()).thenReturn(0.0);
    when(geoLocation.getLatitude()).thenReturn(0.0);
    when(geoLocation.getLongtitude()).thenReturn(0.0);
    when(geoLocation.getPrecision()).thenReturn("world");
  }

  public void setupAttributes() {
    TreeMap<String, List<AttributeValue>> attributesMap = new TreeMap();
    List<AttributeValue> attributeValues = new ArrayList<>();
    attributeValues.add(attributeValue);
    attributesMap.put("attribute", attributeValues);
    when(attributes.getAttributes()).thenReturn(attributesMap);
  }

  public void setupAttributeVlaue() {
    when(attributeValue.getType()).thenReturn("type");
    when(attributeValue.getValue()).thenReturn("typeValue");
  }

  private String getFileFromResources(String path) throws IOException {
    BufferedReader br =
        new BufferedReader(
            new InputStreamReader(new ClassPathResource(path).getInputStream()), 4096);
    StringBuilder stringBuilder = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      stringBuilder.append(line).append('\n');
    }
    br.close();
    String expectedJson = stringBuilder.toString();
    return expectedJson;
  }
}
