package uk.ac.ebi.biosamples.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.model.Sample;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SampleSubmissionService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Traverson traverson;
    private final ExecutorService executor;
    private final RestOperations restOperations;

    public SampleSubmissionService(RestOperations restOperations, Traverson traverson, ExecutorService executor) {
        this.restOperations = restOperations;
        this.traverson = traverson;
        this.executor = executor;
    }

    /**
     * This will send the sample to biosamples, either by POST if it has no
     * accession or by PUT if the sample already has an accession associated
     * <p>
     * This method will wait for the request to complete before returning
     *
     * @param sample sample to be submitted
     * @return sample wrapped in resource
     */
    public Resource<Sample> submit(Sample sample, Boolean setUpdateDate, Boolean setFullDetails) throws RestClientException {
        try {
            return new SubmitCallable(sample, setUpdateDate, setFullDetails).call();
        } catch (RestClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param jwt json web token authorizing access to the domain the sample is assigned to
     */
    public Resource<Sample> submit(Sample sample, String jwt, Boolean setFullDetails) throws RestClientException {
        try {
            return new SubmitCallable(sample, jwt, setFullDetails).call();
        } catch (RestClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This will send the sample to biosamples, either by POST if it has no
     * accession or by PUT if the sample already has an accession associated
     * <p>
     * This will use a thread-pool within the client to do this asyncronously
     *
     * @param sample sample to be submitted
     * @return sample wrapped in resource
     */
    public Future<Resource<Sample>> submitAsync(Sample sample, Boolean setUpdateDate, Boolean setFullDetails) throws RestClientException {
        return executor.submit(new SubmitCallable(sample, setUpdateDate, setFullDetails));
    }

    /**
     * @param jwt json web token authorizing access to the domain the sample is assigned to
     */
    public Future<Resource<Sample>> submitAsync(Sample sample, String jwt, Boolean setFullDetails) throws RestClientException {
        return executor.submit(new SubmitCallable(sample, jwt, setFullDetails));
    }

    private class SubmitCallable implements Callable<Resource<Sample>> {
        private final Sample sample;
        private final Boolean setFullDetails;
        private final String jwt;

        public SubmitCallable(Sample sample, Boolean setUpdateDate, Boolean setFullDetails) {
            this.sample = sample;
            this.setFullDetails = setFullDetails;
            this.jwt = null;
        }

        public SubmitCallable(Sample sample, String jwt, boolean setFullDetails) {
            this.sample = sample;
            this.setFullDetails = setFullDetails;
            this.jwt = jwt;
        }

        @Override
        public Resource<Sample> call() throws Exception {
            // if the sample has an accession, put to that
            if (sample.getAccession() != null) {
                // samples with an existing accession should be PUT

                //don't do all this in traverson because it will get the end and then use the self link
                //because we might PUT to something that doesn't exist (e.g. migration of data)
                //this will cause an error. So instead manually de-template the link without getting it.
                PagedResources<Resource<Sample>> pagedSamples = traverson.follow("samples")
                        .toObject(new ParameterizedTypeReference<PagedResources<Resource<Sample>>>() {
                        });
                Link sampleLink = pagedSamples.getLink("sample");
                if (sampleLink == null) {
                    log.warn("Problem handling page " + pagedSamples);
                    throw new NullPointerException("Unable to find sample link");
                }
                sampleLink = sampleLink.expand(sample.getAccession());
                URI uri = getSamplePersistURI(sampleLink);
                log.trace("PUTing to " + uri + " " + sample);

                RequestEntity<Sample> requestEntity = RequestEntity.put(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON)
                        .body(sample);

                if (jwt != null) {
                    requestEntity.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
                }

                ResponseEntity<Resource<Sample>> responseEntity;
                try {
                    responseEntity = restOperations.exchange(requestEntity,
                            new ParameterizedTypeReference<Resource<Sample>>() {
                            });
                } catch (RestClientResponseException e) {
                    log.error("Unable to PUT to "+uri+" body "+sample+" got response "+e.getResponseBodyAsString());
                    throw e;
                }
                return responseEntity.getBody();

            } else {
                // samples without an existing accession should be POST
                Link sampleLink = traverson.follow("samples").asLink();
                URI uri = getSamplePersistURI(sampleLink);
                log.trace("POSTing to " + uri + " " + sample);

                RequestEntity<Sample> requestEntity = RequestEntity.post(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_JSON)
                        .body(sample);

                if (jwt != null) {
                    requestEntity.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
                }

                ResponseEntity<Resource<Sample>> responseEntity;
                try {
                    responseEntity = restOperations.exchange(requestEntity,
                            new ParameterizedTypeReference<Resource<Sample>>() {
                            });
                } catch (RestClientResponseException e) {
                    log.error("Unable to POST to "+uri+" body "+sample+" got response "+e.getResponseBodyAsString());
                    throw e;
                }

                return responseEntity.getBody();
            }
        }

        private URI getSamplePersistURI(Link sampleLink) {
            UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(sampleLink.getHref());
            if (setFullDetails != null) {
                uriComponentsBuilder.queryParam("setfulldetails", setFullDetails);
            }
            return uriComponentsBuilder.build(true).toUri();
        }
    }
}
