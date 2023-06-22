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
package uk.ac.ebi.biosamples.ega;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class EgaSampleExporter {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String EGA_SAMPLE_URL = "https://ega-archive.org/metadata/v2/samples/";
  private static final String EGA_DATASET_URL =
      "https://ega-archive.org/metadata/v2/datasets?queryBy=sample&queryId=";
  private static final String EGA_DUO_URL = "https://ega-archive.org/catalog5/api/datasets/";
  private final RestTemplate restTemplate;
  private final BioSamplesClient bioSamplesClient;

  public EgaSampleExporter(
      final RestTemplateBuilder restTemplateBuilder, final BioSamplesClient bioSamplesClient) {
    restTemplate = restTemplateBuilder.build();
    this.bioSamplesClient = bioSamplesClient;
  }

  public Void populateAndSubmitEgaData(final String egaId) {
    try {
      final EgaSample egaSample = getEgaSample(egaId);
      final Sample bioSample = getBioSample(egaSample.getBiosampleId());
      final List<String> egaDatasetIds = getEgaDatasets(egaSample.getEgaId());
      final Sample.Builder sampleBuilder = populateSample(bioSample, egaSample);
      populateReferences(sampleBuilder, egaId, egaDatasetIds);
      final Sample sample = sampleBuilder.build();
      updateSample(sample);
      log.info("EGA sample imported: {}", sample.getAccession());
    } catch (final Exception e) {
      log.warn("Failed to import EGA sample egaId: {}", egaId, e);
    }

    return null;
  }

  private EgaSample getEgaSample(final String egaId) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    final ResponseEntity<EgaResponse> response =
        restTemplate.exchange(
            EGA_SAMPLE_URL + egaId,
            HttpMethod.GET,
            new HttpEntity<Void>(headers),
            EgaResponse.class);

    if (response.getStatusCode() != HttpStatus.OK) {
      throw new RuntimeException("Could not retrieve EGA sample");
    }

    final EgaSample egaSample = new EgaSample();
    if (Objects.requireNonNull(response.getBody()).getResponse().getNumTotalResults() == 1) {
      final Result result = response.getBody().getResponse().getResult().get(0);
      egaSample.setEgaId(result.getEgaStableId());
      egaSample.setBiosampleId(result.getBioSampleId());
      egaSample.setSubjectId(result.getSubjectId());
      egaSample.setGender(result.getGender());
      egaSample.setPhenotype(result.getPhenotype());
    } else {
      throw new RuntimeException("Could not retrieve EGA sample");
    }

    return egaSample;
  }

  private Sample getBioSample(final String biosampleId) {
    final Optional<EntityModel<Sample>> sample =
        bioSamplesClient.fetchSampleResource(
            biosampleId, Optional.of(Collections.singletonList("")));
    return sample
        .map(EntityModel::getContent)
        .orElseThrow(() -> new RuntimeException("Could not retrieve BioSamples"));
  }

  private List<String> getEgaDatasets(final String egaId) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    final ResponseEntity<EgaResponse> response =
        restTemplate.exchange(
            EGA_DATASET_URL + egaId,
            HttpMethod.GET,
            new HttpEntity<Void>(headers),
            EgaResponse.class);
    if (response.getStatusCode() != HttpStatus.OK) {
      throw new RuntimeException("Could not retrieve EGA sample");
    }

    final List<String> datasetIds;
    if (Objects.requireNonNull(response.getBody()).getResponse().getNumTotalResults() >= 1) {
      datasetIds =
          response.getBody().getResponse().getResult().stream()
              .map(Result::getEgaStableId)
              .collect(Collectors.toList());
    } else {
      datasetIds = Collections.emptyList();
    }

    return datasetIds;
  }

  private List<String> getDuoCodes(final String egaDatasetId) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    final ResponseEntity<EgaDatasetResponse> response =
        restTemplate.exchange(
            EGA_DUO_URL + egaDatasetId,
            HttpMethod.GET,
            new HttpEntity<Void>(headers),
            EgaDatasetResponse.class);
    if (response.getStatusCode() != HttpStatus.OK) {
      throw new RuntimeException("Could not retrieve EGA dataset");
    }

    final List<DataUseCondition> duoCodes;
    if (Objects.requireNonNull(response.getBody()).getDataUseConditions() != null
        && !response.getBody().getDataUseConditions().isEmpty()) {
      duoCodes = response.getBody().getDataUseConditions();
    } else {
      duoCodes = Collections.emptyList();
    }

    return duoCodes.stream()
        .map(c -> c.getOntology() + ":" + c.getCode())
        .collect(Collectors.toList());
  }

  private Sample.Builder populateSample(final Sample sample, final EgaSample egaSample) {
    final Set<Attribute> attributes = sample.getAttributes();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(
        Attribute.build(
            "gender", egaSample.getGender(), getGenderIri(egaSample.getGender()), null));
    attributes.add(Attribute.build("subjectId", egaSample.getSubjectId(), null, null));
    // attributes.add(Attribute.build("phenotype", egaSample.getPhenotype(), null, null));
    // //uncomment when EGA ready to share phenotype

    return Sample.Builder.fromSample(sample).withAttributes(attributes).withRelease(Instant.now());
  }

  private void populateReferences(
      final Sample.Builder sampleBuilder, final String egaId, final List<String> egaDatasetIds) {
    sampleBuilder.addExternalReference(ExternalReference.build(EGA_SAMPLE_URL + egaId));

    for (final String egaDatasetId : egaDatasetIds) {
      final List<String> duoCodes = getDuoCodes(egaDatasetId);
      sampleBuilder.addExternalReference(
          ExternalReference.build(
              "https://ega-archive.org/datasets/" + egaDatasetId, new TreeSet<>(duoCodes)));
    }
  }

  private void updateSample(final Sample sample) {
    bioSamplesClient.persistSampleResource(sample);
  }

  private static String getGenderIri(final String gender) {
    String genderIri = null;
    if ("female".equalsIgnoreCase(gender)) {
      genderIri = "http://purl.obolibrary.org/obo/PATO_0000383";
    } else if ("male".equalsIgnoreCase(gender)) {
      genderIri = "http://purl.obolibrary.org/obo/PATO_0000384";
    }
    return genderIri;
  }
}
