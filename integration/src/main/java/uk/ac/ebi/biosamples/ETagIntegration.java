package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ETagIntegration extends AbstractIntegration {

    private final BioSamplesProperties bioSamplesProperties;
    private final RestTemplate restTemplate;

    private Logger log = LoggerFactory.getLogger(getClass());

    public ETagIntegration(BioSamplesClient client, BioSamplesProperties bioSamplesProperties, RestTemplateBuilder restTemplateBuilder) {
        super(client);
        this.bioSamplesProperties = bioSamplesProperties;
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    protected void phaseOne() {
        Sample testSample = getTestSample();

        log.info("Submitting sample for ETAG check");

        Resource<Sample> resource = client.persistSampleResource(testSample);
        if (!testSample.equals(resource.getContent())) {
            throw new RuntimeException("Expected response ("+resource.getContent()+") to equal submission ("+testSample+")");
        }

    }

    @Override
    protected void phaseTwo() {
        log.info("Verifying that retrieving ETAG for a sample multiple times doesn't change the ETAG value");
        Sample testSample = getTestSample();

        RequestEntity request = prepareGetRequestForSample(testSample);

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Sample with accession " + testSample.getAccession() + " not found");
        }

        String etag = response.getHeaders().getETag();

        // Fetch the sample another time and check the ETAG
        response = restTemplate.exchange(request, String.class);
        if (!etag.equalsIgnoreCase(response.getHeaders().getETag())) {
            throw new RuntimeException("ETAG for the same object are not identical: " + etag + ", " + response.getHeaders().getETag());
        }

        log.info("Verifying that using the ETAG in a conditional header will return 304 - Not modified");

        RequestEntity.HeadersBuilder requestWithEtagHeader = prepareGetRequestBuilder(testSample);
        RequestEntity etagRequestEntity = requestWithEtagHeader.header("If-None-Match", etag).build();

        response = restTemplate.exchange(etagRequestEntity, String.class);
        if (response.getStatusCode() != HttpStatus.NOT_MODIFIED) {
            throw new RuntimeException("Request using ETAG on a non modified sample did not return the expected status code");
        }


    }

    @Override
    protected void phaseThree() {
        // Verify a put request produces a different ETAG
        log.info("Verifying ETAG of a sample pre and post update are different");
        Sample testSample = getTestSample();

        RequestEntity request = prepareGetRequestForSample(testSample);
        ResponseEntity response = restTemplate.exchange(request, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Sample with accession " + testSample.getAccession() + " not found");
        }

        String etag = response.getHeaders().getETag();

        Sample updatedSample = Sample.Builder.fromSample(testSample)
                .addAttribute(Attribute.build("Organism part", "liver"))
                .build();

        client.persistSampleResource(updatedSample);

        response = restTemplate.exchange(request, String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Sample with accession " + testSample.getAccession() + " not found");
        }

        String newEtag = response.getHeaders().getETag();
        if (etag.equalsIgnoreCase(newEtag)) {
            throw new RuntimeException("A different ETag is expected after a sample update");
        }

    }

    @Override
    protected void phaseFour() {
        log.info("Verifying ETAG of a sample pre and post curation are different, but ETAG of the raw sample remains the same");
        Sample testSample = getTestSample();

        ResponseEntity<String> rawSampleResponse = restTemplate.exchange(prepareGetRequestForRawSample(testSample), String.class);
        ResponseEntity<String> sampleResponse = restTemplate.exchange(prepareGetRequestForSample(testSample), String.class);

        String rawEtag = rawSampleResponse.getHeaders().getETag();
        String eTag = sampleResponse.getHeaders().getETag();

        //This can't be tested in the integration tests because the curation pipelines doesn't run between phases
//        if (rawEtag.equalsIgnoreCase(eTag)) {
//            log.warn("This is suspicious since raw and final sample usually are different due to automatic curation");
//        }

        Curation sampleCuration = Curation.build(
                Stream.of(testSample.getAttributes().first()).collect(Collectors.toSet()),
                Stream.of(Attribute.build("organism", "Homo Sapiens")).collect(Collectors.toSet()));

        client.persistCuration(testSample.getAccession(), sampleCuration, "self.BiosampleIntegrationTest");

        //Fetch again both the sample and the raw sample, the raw ETAG should match
        rawSampleResponse = restTemplate.exchange(prepareGetRequestForRawSample(testSample), String.class);
        sampleResponse = restTemplate.exchange(prepareGetRequestForSample(testSample), String.class);

        String newRawETag = rawSampleResponse.getHeaders().getETag();
        String newEtag = sampleResponse.getHeaders().getETag();

        if (newEtag.equalsIgnoreCase(eTag)) {
            throw new RuntimeException("The ETag of curated sample should be different from the non curated sample");
        }

        if (!rawEtag.equalsIgnoreCase(newRawETag)) {
            throw new RuntimeException("The ETag for the raw sample should not change even after curations");
        }


    }

    @Override
    protected void phaseFive() {

    }

    private Sample getTestSample() {
        return new Sample.Builder("ETAG sample test")
                .withAccession("SAMETAG2031")
                .withDomain("self.BiosampleIntegrationTest")
                .withRelease("2017-01-01T12:00:00")
                .withUpdate("2017-01-01T12:00:00")
                .addAttribute(Attribute.build("organism", "human"))
                .build();

    }

    private RequestEntity prepareGetRequestForSample(Sample sample) {
        Link sampleLink = new Traverson(bioSamplesProperties.getBiosamplesClientUri(), MediaTypes.HAL_JSON)
                .follow("samples")
                .follow(Hop.rel("sample").withParameter("accession", sample.getAccession()))
                .asLink();

        return RequestEntity.get(URI.create(sampleLink.getHref()))
                .accept(MediaTypes.HAL_JSON)
                .build();
    }

    private RequestEntity.HeadersBuilder prepareGetRequestBuilder(Sample sample) {
        Link sampleLink = new Traverson(bioSamplesProperties.getBiosamplesClientUri(), MediaTypes.HAL_JSON)
                .follow("samples")
                .follow(Hop.rel("sample").withParameter("accession", sample.getAccession()))
                .asLink();

        return RequestEntity.get(URI.create(sampleLink.getHref()))
                .accept(MediaTypes.HAL_JSON);

    }

    private RequestEntity prepareGetRequestForRawSample(Sample sample) {
        Link sampleLink = new Traverson(bioSamplesProperties.getBiosamplesClientUri(), MediaTypes.HAL_JSON)
                .follow("samples")
                .follow(Hop.rel("sample").withParameter("accession", sample.getAccession()))
                .follow(Hop.rel("curationDomain").withParameter("curationdomain", Collections.EMPTY_LIST))
                .asLink();

        return RequestEntity.get(URI.create(sampleLink.getHref()))
                .accept(MediaTypes.HAL_JSON).build();

    }
}
