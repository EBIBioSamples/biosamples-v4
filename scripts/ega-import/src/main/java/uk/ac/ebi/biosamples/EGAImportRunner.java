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
package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ols.OlsProcessor;
import uk.ac.ebi.biosamples.ols.OlsResult;

@Component
public class EGAImportRunner implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(EGAImportRunner.class);
  private static final String EGA_DATASET_BASE_URL = "https://ega-archive.org/datasets/";
  private static final String EGA_SAMPLE_BASE_URL = "https://ega-archive.org/metadata/v2/samples/";
  private static final Set<String> UNKNOWN_TERMS =
      new HashSet<>(
          Arrays.asList(
              "n/a",
              "na",
              "n.a",
              "none",
              "unknown",
              "--",
              ".",
              "null",
              "missing",
              "[not reported]",
              "[not requested]",
              "not applicable",
              "not_applicable",
              "not collected",
              "not specified",
              "not known",
              "not reported",
              "missing: not provided"));
  private static final String ATTRIBUTE_PHENOTYPE = "phenotype";
  private static final String ATTRIBUTE_SEX = "sex";

  private final Attribute organism;
  private final BioSamplesClient bioSamplesClient;
  private final OlsProcessor olsProcessor;

  @Autowired
  public EGAImportRunner(final BioSamplesClient bioSamplesClient, final OlsProcessor olsProcessor) {
    this.bioSamplesClient = bioSamplesClient;
    this.olsProcessor = olsProcessor;

    organism =
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null);
  }

  @Override
  public void run(final ApplicationArguments args) {
    if (args.getSourceArgs().length < 1) {
      LOG.error("Please specify a data folder as a program argument");
      throw new IllegalArgumentException("Please specify a data folder as a program argument");
    }

    final String dataFolderUrl = args.getSourceArgs()[0];
    final String datasetDuoUrl = dataFolderUrl + "datasets_duo.csv";
    final String sampleDataUrl = dataFolderUrl + "sanger_released_samples.csv";

    final Map<String, SortedSet<String>> datasetToDuoCodesMap = loadDuoCodeMap(datasetDuoUrl);
    //        Map<String, List<OlsResult>> phenotypeIriMap =
    // loadPhenotypeIriMap(phenotypeIriFile);
    final Map<String, List<OlsResult>> phenotypeIriMap =
        new HashMap<>(); // todo remove this and uncomment above

    try (final BufferedReader br = new BufferedReader(new FileReader(sampleDataUrl))) {
      String line = br.readLine(); // ignore header
      LOG.info("Reading file: {}, headers: {}", sampleDataUrl, line);
      while ((line = br.readLine()) != null && !line.isEmpty()) {
        final String[] sampleValues = line.split(",");
        final String accession = sampleValues[0];
        final String egaId = sampleValues[1];
        final String datasetId = sampleValues[2];
        final String phenotype = sampleValues[3];
        final String sex = sampleValues[4];
        final SortedSet<String> duoCodes = datasetToDuoCodesMap.get(datasetId);
        final List<OlsResult> phenotypeIris = phenotypeIriMap.get(phenotype);

        processSampleRecord(accession, egaId, datasetId, phenotype, sex, duoCodes, phenotypeIris);
      }
    } catch (final JsonProcessingException e) {
      LOG.error("JSON conversion failure", e);
    } catch (final IOException e) {
      LOG.error("Couldn't read file: " + datasetDuoUrl, e);
    }
  }

  private void processSampleRecord(
      final String accession,
      final String egaId,
      final String datasetId,
      final String phenotype,
      final String sex,
      final SortedSet<String> duoCodes,
      final List<OlsResult> phenotypeIris)
      throws JsonProcessingException {

    final ObjectMapper jsonMapper = new ObjectMapper();
    final Optional<EntityModel<Sample>> sampleResource =
        bioSamplesClient.fetchSampleResource(accession);
    if (sampleResource.isPresent()) {
      final Sample sample = sampleResource.get().getContent();
      LOG.info("Original sample: {}", jsonMapper.writeValueAsString(sample));
      if (sample.getAttributes().size() != 2) {
        LOG.warn("Attributes size != 2, Attributes {}", sample.getAttributes());
      }

      // remove extra attributes from migration (deleted and other-migrated from....)
      removeMigrationRelatedAttributes(sample);

      final Sample.Builder sampleBuilder =
          Sample.Builder.fromSample(sample)
              .addAttribute(Attribute.build("ega dataset id", datasetId))
              .addAttribute(Attribute.build("ega sample id", egaId))
              .addAttribute(organism)
              .addExternalReference(
                  ExternalReference.build(EGA_DATASET_BASE_URL + datasetId, duoCodes))
              .addExternalReference(ExternalReference.build(EGA_SAMPLE_BASE_URL + egaId))
              .withRelease(Instant.now());

      // ignore unknown, n/a terms
      if (UNKNOWN_TERMS.contains(phenotype.toLowerCase())) {
        LOG.info("Ignoring phenotype as it contains {}", phenotype);
      } else {
        final Attribute attributePhenotype =
            populateAttribute(phenotype, phenotypeIris, ATTRIBUTE_PHENOTYPE);
        sampleBuilder.addAttribute(attributePhenotype);
      }
      if (UNKNOWN_TERMS.contains(sex.toLowerCase())) {
        LOG.info("Ignoring sex as it contains {}", sex);
      } else {
        final Attribute attributeSex = populateAttribute(sex, getSexOntology(sex), ATTRIBUTE_SEX);
        sampleBuilder.addAttribute(attributeSex);
      }

      final Sample updatedSample = sampleBuilder.build();

      LOG.info("Updated sample: {}", jsonMapper.writeValueAsString(updatedSample));
      bioSamplesClient.persistSampleResource(updatedSample);
    } else {
      LOG.warn("Sample not found in biosamples: {}", accession);
    }
  }

  private Map<String, SortedSet<String>> loadDuoCodeMap(final String datasetDuoUrl) {
    final Map<String, SortedSet<String>> datasetToDuoCodesMap = new HashMap<>();

    try (final BufferedReader br = new BufferedReader(new FileReader(datasetDuoUrl))) {
      String line = br.readLine(); // ignore header
      LOG.info("Reading file: {}, headers: {}", datasetDuoUrl, line);
      while ((line = br.readLine()) != null && !line.isEmpty()) {
        final String[] record = line.replaceAll("[\"\\[\\] ]", "").split(",");
        final String datasetId = record[0];
        final String[] duoCodes = Arrays.copyOfRange(record, 1, record.length);

        datasetToDuoCodesMap.put(
            datasetId,
            new TreeSet<>(
                Arrays.stream(duoCodes).map(s -> "DUO:" + s).collect(Collectors.toList())));
      }

    } catch (final IOException e) {
      LOG.error("couldn't read file: " + datasetDuoUrl, e);
    }
    return datasetToDuoCodesMap;
  }

  private void removeMigrationRelatedAttributes(final Sample sample) {
    final List<Attribute> attributesToRemove = new ArrayList<>();
    for (final Attribute attribute : sample.getAttributes()) {
      if (attribute.getType().equals("deleted")
          || (attribute.getType().equals("other")
              && attribute.getValue().startsWith("migrated from"))) {
        attributesToRemove.add(attribute);
        LOG.info(
            "Removing attribute {}={} from original sample: {}",
            attribute.getType(),
            attribute.getValue(),
            sample.getAccession());
      } else if (attribute.getType().equals("phenotype")) {
        attributesToRemove.add(attribute);
        LOG.warn(
            "Removing attribute phenotype={} from original sample: {}",
            attribute.getValue(),
            sample.getAccession());
      } else if (attribute.getType().equals("organism")) {
        attributesToRemove.add(attribute);
        LOG.warn(
            "Removing attribute organism={} from original sample: {}",
            attribute.getValue(),
            sample.getAccession());
      } else if (attribute.getType().equals("sex")) {
        attributesToRemove.add(attribute);
        LOG.warn(
            "Removing attribute sex={} from original sample: {}",
            attribute.getValue(),
            sample.getAccession());
      }
    }
    for (final Attribute attribute : attributesToRemove) {
      sample.getAttributes().remove(attribute);
    }
  }

  private Attribute populateAttribute(
      final String phenotype, final List<OlsResult> attributeIris, final String attributeType) {
    final Optional<OlsResult> olsMappedTerm = getOlsMappedTerm(phenotype);
    final Attribute attribute;

    final List<String> iris = new ArrayList<>();
    if (attributeIris != null && !attributeIris.isEmpty()) {
      for (final OlsResult o : attributeIris) {
        iris.add(o.getIri());
      }
    }

    if (olsMappedTerm.isPresent()) {
      iris.add(olsMappedTerm.get().getIri());
      attribute = Attribute.build(attributeType, olsMappedTerm.get().getLabel(), null, iris, null);
    } else {
      attribute = Attribute.build(attributeType, phenotype, null, iris, null);
    }

    return attribute;
  }

  private Optional<OlsResult> getOlsMappedTerm(final String termToMap) {
    Optional<OlsResult> olsMappedTerm = Optional.empty();
    if (termToMap.matches("^[A-Za-z]+[_:\\-][0-9]+$")) {
      olsMappedTerm = olsProcessor.queryForOlsObject(termToMap);

      if (olsMappedTerm.isPresent()) {
        LOG.info("OLS mapped term {} into {}", termToMap, olsMappedTerm.get().getIri());
      } else {
        LOG.warn("Could not find term({}) in OLS", termToMap);
      }
    }

    return olsMappedTerm;
  }

  private List<OlsResult> getSexOntology(final String sex) {
    final List<OlsResult> olsResults;
    switch (sex.toLowerCase()) {
      case "male":
        olsResults =
            Collections.singletonList(
                new OlsResult("male", "http://purl.obolibrary.org/obo/PATO_0000384"));
        break;
      case "female":
        olsResults =
            Collections.singletonList(
                new OlsResult("female", "http://purl.obolibrary.org/obo/PATO_0000383"));
        break;
      default:
        olsResults = null;
        break;
    }
    return olsResults;
  }
}
