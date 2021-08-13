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
import java.util.*;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.model.structured.HistologyEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredCell;
import uk.ac.ebi.biosamples.model.structured.StructuredDataType;
import uk.ac.ebi.biosamples.model.structured.StructuredTable;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;
import uk.ac.ebi.biosamples.model.structured.amr.AmrPair;

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
    return new Sample.Builder("FakeSample", "SAMFAKE123456");
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

  public Sample getExampleSampleWithStructuredData() {
    return getExampleSampleWithStructuredDataBuilder().build();
  }

  private Sample.Builder getExampleSampleWithStructuredDataBuilder() {
    final AMREntry amrEntry =
        new AMREntry.Builder()
            .withAntibioticName(new AmrPair("ExampleAntibiotic"))
            .withAstStandard("ExampleASTStandard")
            .withSpecies(new AmrPair("ExampleOrganism"))
            .withLaboratoryTypingMethod("NA")
            .withMeasurement("0")
            .withMeasurementUnits("mg/L")
            .withMeasurementSign("+")
            .withResistancePhenotype("NA")
            .build();
    final AMRTable amrTable =
        new AMRTable.Builder("test", "self.ExampleDomain", null)
            .withEntries(Arrays.asList(amrEntry))
            .build();

    return new Sample.Builder("FakeSampleWithStructuredData", "SAMFAKE123456")
        .withData(Arrays.asList(amrTable));
  }

  public Sample getExampleSampleWithStructuredData2() {
    StructuredTable<HistologyEntry> histologyData =
        new StructuredTable.Builder<HistologyEntry>(
                "www.fake.schema.url",
                "self.ExampleDomain",
                null,
                StructuredDataType.HISTOLOGY_MARKERS)
            .addEntry(
                new HistologyEntry.Builder()
                    .withMarker(new StructuredCell("Crypt depth", ""))
                    .withMeasurement(new StructuredCell("0", ""))
                    .withMeasurementUnits(new StructuredCell("um", ""))
                    .withPartner(
                        new StructuredCell(
                            "FUB",
                            "https://www.fu-berlin.de/en/sites/china/ueberfub/forschung/index.html"))
                    .withMethod(new StructuredCell("TEST", ""))
                    .build())
            .addEntry(
                new HistologyEntry.Builder()
                    .withMarker(
                        new StructuredCell(
                            "Villous height", "http://purl.obolibrary.org/obo/NCIT_C14170"))
                    .withMeasurement(new StructuredCell("0", ""))
                    .withMeasurementUnits(new StructuredCell("um", ""))
                    .withPartner(
                        new StructuredCell(
                            "FUB",
                            "https://www.fu-berlin.de/en/sites/china/ueberfub/forschung/index.html"))
                    .build())
            .build();

    return new Sample.Builder("FakeSampleWithStructuredData", "SAMFAKE123456")
        .withData(Arrays.asList(histologyData))
        .build();
  }
}
