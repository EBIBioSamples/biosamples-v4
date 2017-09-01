package uk.ac.ebi.biosamples.client.service;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Sample;

public class SampleSubmissionService {

	private Logger log = LoggerFactory.getLogger(getClass());

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
	 * 
	 * @param sample
	 * @return
	 */
	public Future<Resource<Sample>> submitAsync(Sample sample, Boolean setUpdateDate) throws RestClientException {
		return executor.submit(new SubmitCallable(sample, setUpdateDate));
	}

	private class SubmitCallable implements Callable<Resource<Sample>> {
		private final Sample sample;
		private final Boolean setUpdateDate;

		public SubmitCallable(Sample sample, Boolean setUpdateDate) {
			this.sample = sample;
			this.setUpdateDate = setUpdateDate;
		}

		@Override
		public Resource<Sample> call() throws Exception {
			// if the sample has an accession, put to that
			if (sample.getAccession() != null) {
				// samples with an existing accession should be PUT
				
				//don't do all this in traverson because it will get the end and then use the self link
				//because we might PUT to something that doesn't exist (e.g. migration of data)
				//this will cause an error. So instead manually de-template the link without getting it.
				PagedResources<Resource<Sample>> pagedSamples = traverson.follow("samples").toObject(new ParameterizedTypeReference<PagedResources<Resource<Sample>>>(){});			
				Link sampleLink = pagedSamples.getLink("sample");
				if (sampleLink == null) {
					log.info("Problem handling page "+pagedSamples);
					throw new NullPointerException("Unable to find sample link");
				}
				sampleLink = sampleLink.expand(sample.getAccession());
								
				UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(sampleLink.getHref());
				if (setUpdateDate != null) {
					uriComponentsBuilder.queryParam("setupdatedate", setUpdateDate);
				}
				URI uri = uriComponentsBuilder.build(true).toUri();
				
				log.trace("PUTing to " + uri + " " + sample);
	
				RequestEntity<Sample> requestEntity = RequestEntity.put(uri)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaTypes.HAL_JSON)
						.body(sample);
				ResponseEntity<Resource<Sample>> responseEntity = restOperations.exchange(requestEntity,
						new ParameterizedTypeReference<Resource<Sample>>() {
						});
	
				if (!responseEntity.getStatusCode().is2xxSuccessful()) {
					log.error("Unable to PUT " + sample.getAccession() + " : " + responseEntity.toString());
					throw new RuntimeException("Problem PUTing " + sample.getAccession());
				}
				return responseEntity.getBody();
	
			} else {
				// samples without an existing accession should be POST
				Link sampleLink = traverson.follow("samples").asLink();

				UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(sampleLink.getHref());
				if (setUpdateDate != null) {
					uriComponentsBuilder.queryParam("setupdatedate", setUpdateDate);
				}
				URI uri = uriComponentsBuilder.build(true).toUri();
				
				log.trace("POSTing to " + uri + " " + sample);
	
				RequestEntity<Sample> requestEntity = RequestEntity.post(uri)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaTypes.HAL_JSON)
						.body(sample);
				ResponseEntity<Resource<Sample>> responseEntity = restOperations.exchange(requestEntity,
						new ParameterizedTypeReference<Resource<Sample>>() {
						});
	
				return responseEntity.getBody();
			}
		}
	}
}
