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
      RestTemplateBuilder restTemplateBuilder, BioSamplesClient bioSamplesClient) {
    restTemplate = restTemplateBuilder.build();
    this.bioSamplesClient = bioSamplesClient;
  }

  public Void populateAndSubmitEgaData(String egaId) {
    try {
      EgaSample egaSample = getEgaSample(egaId);
      Sample bioSample = getBioSample(egaSample.getBiosampleId());
      List<String> egaDatasetIds = getEgaDatasets(egaSample.getEgaId());
      Sample.Builder sampleBuilder = populateSample(bioSample, egaSample);
      populateReferences(sampleBuilder, egaId, egaDatasetIds);
      Sample sample = sampleBuilder.build();
      updateSample(sample);
      log.info("EGA sample imported: {}", sample.getAccession());
    } catch (Exception e) {
      log.warn("Failed to import EGA sample egaId: {}", egaId, e);
    }

    return null;
  }

  public EgaSample getEgaSample(String egaId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<EgaResponse> response =
        restTemplate.exchange(
            EGA_SAMPLE_URL + egaId,
            HttpMethod.GET,
            new HttpEntity<Void>(headers),
            EgaResponse.class);

    if (response.getStatusCode() != HttpStatus.OK) {
      throw new RuntimeException("Could not retrieve EGA sample");
    }

    EgaSample egaSample = new EgaSample();
    if (Objects.requireNonNull(response.getBody()).getResponse().getNumTotalResults() == 1) {
      Result result = response.getBody().getResponse().getResult().get(0);
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

  public Sample getBioSample(String biosampleId) {
    Optional<EntityModel<Sample>> sample =
        bioSamplesClient.fetchSampleResource(biosampleId, Optional.of(Collections.emptyList()));
    return sample
        .map(EntityModel::getContent)
        .orElseThrow(() -> new RuntimeException("Could not retrieve BioSamples"));
  }

  public List<String> getEgaDatasets(String egaId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<EgaResponse> response =
        restTemplate.exchange(
            EGA_DATASET_URL + egaId,
            HttpMethod.GET,
            new HttpEntity<Void>(headers),
            EgaResponse.class);
    if (response.getStatusCode() != HttpStatus.OK) {
      throw new RuntimeException("Could not retrieve EGA sample");
    }

    List<String> datasetIds;
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

  public List<String> getDuoCodes(String egaDatasetId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<EgaDatasetResponse> response =
        restTemplate.exchange(
            EGA_DUO_URL + egaDatasetId,
            HttpMethod.GET,
            new HttpEntity<Void>(headers),
            EgaDatasetResponse.class);
    if (response.getStatusCode() != HttpStatus.OK) {
      throw new RuntimeException("Could not retrieve EGA dataset");
    }

    List<DataUseCondition> duoCodes;
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

  public Sample.Builder populateSample(Sample sample, EgaSample egaSample) {
    Set<Attribute> attributes = sample.getAttributes();
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

  public void populateReferences(
      Sample.Builder sampleBuilder, String egaId, List<String> egaDatasetIds) {
    sampleBuilder.addExternalReference(ExternalReference.build(EGA_SAMPLE_URL + egaId));

    for (String egaDatasetId : egaDatasetIds) {
      List<String> duoCodes = getDuoCodes(egaDatasetId);
      sampleBuilder.addExternalReference(
          ExternalReference.build(
              "https://ega-archive.org/datasets/" + egaDatasetId, new TreeSet<>(duoCodes)));
    }
  }

  public void updateSample(Sample sample) {
    bioSamplesClient.persistSampleResource(sample);
  }

  public static String getGenderIri(String gender) {
    String genderIri = null;
    if ("female".equalsIgnoreCase(gender)) {
      genderIri = "http://purl.obolibrary.org/obo/PATO_0000383";
    } else if ("male".equalsIgnoreCase(gender)) {
      genderIri = "http://purl.obolibrary.org/obo/PATO_0000384";
    }
    return genderIri;
  }
}
