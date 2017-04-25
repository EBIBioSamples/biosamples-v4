package uk.ac.ebi.biosamples;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Sample;

@Component
public class BiosamplesRestOperations {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private IntegrationProperties integrationProperties;
    private RestOperations restOperations;

    public BiosamplesRestOperations(RestOperations restOperations, IntegrationProperties properties) {
        this.restOperations = restOperations;
        this.integrationProperties = properties;
    }

    public ResponseEntity<Resource<Sample>> doGet(Sample sample) throws RestClientException {
        URI uri = UriComponentsBuilder.fromUri(this.integrationProperties.getBiosampleSubmissionUri()).pathSegment("samples")
                .pathSegment(sample.getAccession()).build().toUri();

        log.info("GETting from "+uri);
        RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
        ResponseEntity<Resource<Sample>> response = restOperations.exchange(request, new ParameterizedTypeReference<Resource<Sample>>(){});
        return response;
    }

    public Resource<Sample> doPut(Sample sample) throws RestClientException {
        URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri()).pathSegment("samples")
                .pathSegment(sample.getAccession()).build().toUri();

        log.info("PUTting to "+uri);
        RequestEntity<Sample> request = RequestEntity.put(uri).contentType(MediaType.APPLICATION_JSON).body(sample);
        ResponseEntity<Resource<Sample>> response = restOperations.exchange(request, new ParameterizedTypeReference<Resource<Sample>>(){});
        if (!sample.equals(response.getBody().getContent())) {
            log.info("sample = "+sample);
            log.info("response.getBody() = "+response.getBody());
            throw new RuntimeException("Expected response to equal submission");
        }
        return response.getBody();
    }
}
