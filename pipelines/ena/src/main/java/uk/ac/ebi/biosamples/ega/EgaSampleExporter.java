package uk.ac.ebi.biosamples.ega;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class EgaSampleExporter {
    private static final String EGA_SAMPLE_URL = "https://ega-archive.org/metadata/v2/samples/";
    private static final String EGA_DATASET_URL = "https://ega-archive.org/metadata/v2/datasets?queryBy=sample&queryId=";
    private static final String EGA_DUO_URL = "https://ega-archive.org/catalog5/api/datasets/";
    private final RestTemplate restTemplate;
    private final BioSamplesClient bioSamplesClient;

    public EgaSampleExporter(RestTemplateBuilder restTemplateBuilder, BioSamplesClient bioSamplesClient) {
        restTemplate = restTemplateBuilder.build();
        this.bioSamplesClient = bioSamplesClient;
    }

    public static void main(String[] args) {
        String egaId = "EGAD00001006893";
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<EgaDatasetResponse> response = restTemplate.exchange(
                EGA_DUO_URL + egaId, HttpMethod.GET, new HttpEntity<Void>(headers), EgaDatasetResponse.class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new RuntimeException("Could not retrieve EGA sample");
        }

        List<DataUseCondition> duoCodes = null;
        if (response.getBody().getDataUseConditions() != null && !response.getBody().getDataUseConditions().isEmpty()) {
            duoCodes = response.getBody().getDataUseConditions();
        }

        System.out.println(duoCodes);
    }

    public Void populateAndSubmitEgaData(String egaId) {
        EgaSample egaSample = getEgaSample(egaId);
        Sample bioSample = getBioSample(egaSample.getBiosampleId());
        List<String> egaDatasetIds = getEgaDatasets(egaSample.getEgaId());
        Sample.Builder sampleBuilder = populateSample(bioSample, egaSample);
        populateReferences(sampleBuilder, egaId, egaDatasetIds);
        Sample sample = sampleBuilder.build();
        updateSample(sample);

        return null;
    }


    public EgaSample getEgaSample(String egaId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<EgaResponse> response = restTemplate.exchange(
                EGA_SAMPLE_URL + egaId, HttpMethod.GET, new HttpEntity<Void>(headers), EgaResponse.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Could not retrieve EGA sample");
        }

        EgaSample egaSample = new EgaSample();
        if (response.getBody().getResponse().getNumTotalResults() == 1) {
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
        Optional<Resource<Sample>> sample = bioSamplesClient.fetchSampleResource(biosampleId, Optional.of(Collections.emptyList()));
        return sample.map(Resource::getContent).orElseThrow(() -> new RuntimeException("Could not retrieve BioSamples"));
    }

    public List<String> getEgaDatasets(String egaId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<EgaResponse> response = restTemplate.exchange(
                EGA_DATASET_URL + egaId, HttpMethod.GET, new HttpEntity<Void>(headers), EgaResponse.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Could not retrieve EGA sample");
        }

        List<String> datasetIds;
        if (response.getBody().getResponse().getNumTotalResults() >= 1) {
            datasetIds = response.getBody().getResponse().getResult().stream()
                                 .map(Result::getEgaStableId).collect(Collectors.toList());
        } else {
            datasetIds = Collections.emptyList();
        }

        return datasetIds;
    }

    public List<String> getDuoCodes(String egaDatasetId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<EgaDatasetResponse> response = restTemplate.exchange(
                EGA_DUO_URL + egaDatasetId, HttpMethod.GET, new HttpEntity<Void>(headers), EgaDatasetResponse.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Could not retrieve EGA dataset");
        }

        List<DataUseCondition> duoCodes;
        if (response.getBody().getDataUseConditions() != null && !response.getBody().getDataUseConditions().isEmpty()) {
            duoCodes = response.getBody().getDataUseConditions();
        } else {
            duoCodes = Collections.emptyList();
        }

        return duoCodes.stream().map(c -> c.getOntology() + ":" + c.getCode()).collect(Collectors.toList());
    }

    public Sample.Builder populateSample(Sample sample, EgaSample egaSample) {
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
        attributes.add(Attribute.build("gender", egaSample.getGender(), getGenderIri(egaSample.getGender()), null));
        attributes.add(Attribute.build("subjectId", egaSample.getSubjectId(), null, null));
        //attributes.add(Attribute.build("phenotype", egaSample.getPhenotype(), null, null));

        return Sample.Builder.fromSample(sample).withAttributes(attributes).withRelease(Instant.now());
    }

    public void populateReferences(Sample.Builder sampleBuilder, String egaId, List<String> egaDatasetIds) {
        sampleBuilder.addExternalReference(ExternalReference.build(EGA_SAMPLE_URL + egaId));

        for (String egaDatasetId : egaDatasetIds) {
            List<String> duoCodes = getDuoCodes(egaDatasetId);
            sampleBuilder.addExternalReference(ExternalReference.build(
                    "https://ega-archive.org/datasets/" + egaDatasetId, new TreeSet<>(duoCodes)));
            if (!duoCodes.isEmpty()) {
                System.out.println(sampleBuilder.build());
            }
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
