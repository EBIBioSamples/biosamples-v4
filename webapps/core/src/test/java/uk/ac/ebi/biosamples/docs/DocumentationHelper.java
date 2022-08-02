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
package uk.ac.ebi.biosamples.docs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;

public class DocumentationHelper {

  //    private final String[] sampleAccessionPrefix = {"SAME", "SAMD", "SAMEA", "SAMN"};
  private final int maxRandomNumber = 100000;

  private final Random randomGenerator = new Random(new Date().toInstant().toEpochMilli());

  public List<Sample> generateRandomSamples(int numberOfSamples) {

    List<Sample> samples = new ArrayList<>();
    Set<String> usedAccession = new HashSet<>();
    String sampleAccession = null;

    for (int i = 0; i < numberOfSamples; i++) {

      while (sampleAccession == null || usedAccession.contains(sampleAccession)) {

        int randomInt = randomGenerator.nextInt(maxRandomNumber);
        sampleAccession = String.format("%s%06d", "SAMFAKE", randomInt);
        //                String randomPrefix = sampleAccessionPrefix[randomInt %
        // sampleAccessionPrefix.length];
      }

      usedAccession.add(sampleAccession);
      Sample sample = new Sample.Builder("FakeSample", sampleAccession).build();

      samples.add(sample);
    }

    return samples;
  }

  public Sample generateRandomSample() {
    return this.generateRandomSamples(1).get(0);
  }

  public String generateRandomDomain() {
    List<String> domainNames =
        Arrays.asList(
            "self.BioSamples",
            "self.USI",
            "self.ENA",
            "self.ArrayExpress",
            "self.EVA",
            "self.FAANG",
            "self.HipSci",
            "self.EBiSC");

    return domainNames.get(randomGenerator.nextInt(domainNames.size()));
  }

  public String getExampleDomain() {
    return "self.ExampleDomain";
  }

  public Sample.Builder getExampleSampleBuilder() {
    return new Sample.Builder("FakeSample", "SAMEA12345");
  }

  public Sample getExampleSample() {
    return getExampleSampleBuilder().build();
  }

  public Sample getExampleSampleWithDomain() {
    return getExampleSampleBuilder().withDomain(getExampleDomain()).build();
  }

  public Sample getExampleSampleWithWebinId() {
    return getExampleSampleBuilder().withWebinSubmissionAccountId("WEBIN-12345").build();
  }

  public Sample getExampleSampleWithoutWebinId() {
    return getExampleSampleBuilder().build();
  }

  public Sample getExampleSampleWithExternalReferences() {
    return getExampleSampleBuilder()
        .addExternalReference(
            ExternalReference.build("https://www.ebi.ac.uk/ena/data/view/SAMEA00001"))
        .withDomain(getExampleDomain())
        .build();
  }

  public Sample getExampleSampleWithRelationships() {
    return getExampleSampleBuilder()
        .addRelationship(Relationship.build("SAMFAKE123456", "derived from", "SAMFAKE654321"))
        .withDomain(getExampleDomain())
        .build();
  }

  public Curation getExampleCuration() {
    Curation curationObject =
        Curation.build(
            Collections.singletonList(Attribute.build("Organism", "Human", "9606", null)),
            Collections.singletonList(
                Attribute.build(
                    "Organism",
                    "Homo sapiens",
                    "http://purl.obolibrary.org/obo/NCBITaxon_9606",
                    null)),
            Collections.singletonList(ExternalReference.build("www.google.com")),
            Collections.singletonList(ExternalReference.build("www.ebi.ac.uk/ena/ERA123456")),
            Collections.emptyList(),
            Collections.singletonList(
                Relationship.build("SAMFAKE123456", "DERIVED_FROM", "SAMFAKE7654321")));

    return curationObject;
  }

  public CurationLink getExampleCurationLink() {
    Curation curationObject = getExampleCuration();
    Sample sampleObject = getExampleSampleBuilder().build();
    String domain = getExampleDomain();

    return CurationLink.build(
        sampleObject.getAccession(), curationObject, domain, null, Instant.now());
  }

  public CurationLink getExampleCurationLinkWithWebinId() {
    Curation curationObject = getExampleCuration();
    Sample sampleObject = getExampleSampleBuilder().build();

    return CurationLink.build(
        sampleObject.getAccession(), curationObject, null, "WEBIN-12345", Instant.now());
  }

  public StructuredData getExampleStructuredData() {
    Set<StructuredDataTable> structuredDataTableSet = new HashSet<>();
    Set<Map<String, StructuredDataEntry>> dataContent = new HashSet<>();
    Map<String, StructuredDataEntry> dataMap = new HashMap<>();
    dataMap.put(
        "Marker", StructuredDataEntry.build("value_1", "http://purl.obolibrary.org/obo/value_1"));
    dataMap.put("Measurement", StructuredDataEntry.build("value_1", null));
    dataMap.put("Measurement Units", StructuredDataEntry.build("value_1", null));
    dataMap.put("Partner", StructuredDataEntry.build("value_1", null));
    dataMap.put("Method", StructuredDataEntry.build("value_1", null));
    dataContent.add(dataMap);
    structuredDataTableSet.add(
        StructuredDataTable.build("self.ExampleDomain", null, "CHICKEN_DATA", null, dataContent));

    dataContent = new HashSet<>();
    dataMap = new HashMap<>();
    dataMap.put(
        "antibioticName",
        StructuredDataEntry.build("nalidixic acid", "http://purl.obolibrary.org/obo/value_1"));
    dataMap.put("resistancePhenotype", StructuredDataEntry.build("intermediate", null));
    dataMap.put("measurementSign ", StructuredDataEntry.build("==", null));
    dataMap.put("measurement", StructuredDataEntry.build("17", null));
    dataMap.put("measurementUnits", StructuredDataEntry.build("mm", null));
    dataMap.put("laboratoryTypingMethod", StructuredDataEntry.build("disk diffusion", null));
    dataMap.put("platform", StructuredDataEntry.build("missing", null));
    dataMap.put(
        "laboratoryTypingMethodVersionOrReagent", StructuredDataEntry.build("missing", null));
    dataMap.put("vendor", StructuredDataEntry.build("Becton Dickinson", null));
    dataMap.put("astStandard", StructuredDataEntry.build("CLSI", null));
    dataContent.add(dataMap);
    structuredDataTableSet.add(
        StructuredDataTable.build("self.ExampleDomain", null, "AMR", null, dataContent));

    return StructuredData.build("SAMFAKE123456", Instant.now(), structuredDataTableSet);
  }
}
