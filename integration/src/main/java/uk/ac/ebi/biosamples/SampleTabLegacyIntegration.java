package uk.ac.ebi.biosamples;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component
@Order(5)
@Profile({"default", "test"})
public class SampleTabLegacyIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final IntegrationProperties integrationProperties;

	private final RestOperations restTemplate;

	private final URI uriSb;
	private final URI uriVa;
	private final URI uriAc;
	private final URI fileUriSb;
	
	public SampleTabLegacyIntegration(RestTemplateBuilder restTemplateBuilder, IntegrationProperties integrationProperties, BioSamplesClient client) {
        super(client);
		this.restTemplate = restTemplateBuilder.build();
		this.integrationProperties = integrationProperties;

		uriVa = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
				.pathSegment("api", "v1", "json", "va").build().toUri();
		uriAc = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
				.pathSegment("api", "v1", "json", "ac")
				.queryParam("apikey", integrationProperties.getLegacyApiKey())
				.build().toUri();
		uriSb = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
				.pathSegment("api", "v1", "json", "sb")
				.queryParam("apikey", integrationProperties.getLegacyApiKey())
				.build().toUri();

		fileUriSb = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
				.pathSegment("api", "v1", "file", "sb")
				.queryParam("apikey", integrationProperties.getLegacyApiKey())
				.build().toUri();
	}

	@Override
	protected void phaseOne() {
		log.info("Testing SampleTab JSON validation");
		runCallableOnSampleTabResource("/GSB-32.json", sampleTabString -> {
			log.info("POSTing to " + uriVa);
			RequestEntity<String> request = RequestEntity.post(uriVa).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			log.info(""+response.getBody());
		});

		log.info("Testing SampleTab JSON accession");
		runCallableOnSampleTabResource("/GSB-32.json", sampleTabString -> {
			log.info("POSTing to " + uriAc);
			RequestEntity<String> request = RequestEntity.post(uriAc).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			log.info(""+response.getBody());

			if (!response.getBody().contains("SAMEA2186845")) {
				throw new RuntimeException("Response does not have expected accession SAMEA2186845");
			}
		});

		log.info("Testing SampleTab JSON submission");
		runCallableOnSampleTabResource("/GSB-32.json", sampleTabString -> {
			log.info("POSTing to " + uriSb);
			RequestEntity<String> request = RequestEntity.post(uriSb).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			log.info(""+response.getBody());

			if (!response.getBody().contains("SAMEA2186845")) {
				throw new RuntimeException("Response does not have expected accession SAMEA2186845");
			}
		});

		log.info("Testing SampleTab JSON submission with MSI contact, publication and organization");
		runCallableOnSampleTabResource("/GSB-1010.json", sampleTabString -> {
			log.info("POSTing to " + uriSb);
			RequestEntity<String> request = RequestEntity.post(uriSb).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			log.info(""+response.getBody());
		});

		log.info("Testing unaccessioned SampleTab JSON submission ");
		runCallableOnSampleTabResource("/GSB-new.json", sampleTabString -> {
			log.info("POSTing to " + uriSb);
			RequestEntity<String> request = RequestEntity.post(uriSb).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			//TODO do this check better
			if (!response.getBody().contains("GSB-")) {
				throw new RuntimeException("Response does not have expected submission identifier");
			}
			log.info(""+response.getBody());
		});

		log.info("Testing submission of sampletab with valid implicit relationship");
		runCallableOnSampleTabFileResource("/Implicit_relationship_sampletab.txt", sampletabFile -> {
			log.info("POSTing to " + fileUriSb);
			LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("file", sampletabFile);
			RequestEntity<LinkedMultiValueMap> request = RequestEntity.post(fileUriSb).contentType(MediaType.MULTIPART_FORM_DATA)
					.body(map);

			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			if (!response.getBody().contains("GSB-")) {
				log.error(response.toString());
				throw new RuntimeException("Response does not have expected submission identifier");
			}
			log.info(""+response.getBody());
		});

		log.info("Testing rejection submission of sampletab with invalid implicit relationship");
		runCallableOnSampleTabFileResource("/Invalid_implicit_relationship_sampletab.txt", sampletabFile -> {
			log.info("POSTing to " + fileUriSb);
			LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("file", sampletabFile);
			RequestEntity<LinkedMultiValueMap> request = RequestEntity.post(fileUriSb).contentType(MediaType.MULTIPART_FORM_DATA)
					.body(map);

			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			if (!response.getBody().contains("Unable to accession")) {
				log.error(response.toString());
				throw new RuntimeException("Response does not have expected submission identifier");
			}
			log.info("SampleTab with invalid relation has been rejected as expected");
			log.info(""+response.getBody());
		});

	}

	@Override
	protected void phaseTwo() {
		
		//check that previous submitted GSB-32 samples have been put into a group
		
		Optional<Sample> SAMEA2186845 = client.fetchSample("SAMEA2186845");
		if (!SAMEA2186845.isPresent()) {
			throw new RuntimeException("Unable to fetch SAMEA2186845");
		} else if (SAMEA2186845.get().getRelationships().size() == 0) {
			throw new RuntimeException("SAMEA2186845 has no relationships");	
		} else {
			boolean inGroup = false;
			for (Relationship r : SAMEA2186845.get().getRelationships()) {
				if (r.getTarget().equals("SAMEA2186845") && r.getType().equals("has member")) {
					inGroup = true;
					if (!r.getSource().startsWith("SAMEG")) {
						throw new RuntimeException("SAMEA2186845 has 'has member' relationship to something not SAMEG");
					}
				}
				//check each relationship has a sane source and target
				if (!r.getTarget().matches("SAM[END][AG]?[0-9]+")) {
					throw new RuntimeException("SAMEA2186845 has relationship to "+r.getTarget());
				}
				if (!r.getSource().matches("SAM[END][AG]?[0-9]+")) {
					throw new RuntimeException("SAMEA2186845 has relationship from "+r.getSource());
				}
			}
			if (!inGroup) {
				throw new RuntimeException("SAMEA2186845 has no 'has member' relationship to it");
			}
		}

		log.info("Testing SampleTab JSON accession unaccessioned");
		runCallableOnSampleTabResource("/GSB-32_unaccession.json", sampleTabString -> {
			log.info("POSTing to " + uriAc);
			RequestEntity<String> request = RequestEntity.post(uriAc).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			log.info(""+response.getBody());
			
			if (!response.getBody().contains("SAMEA2186845")) {
				throw new RuntimeException("Response does not have expected accession SAMEA2186845");
			}
			
		});	
		
		log.info("Testing SampleTab JSON submission unaccessioned");
		runCallableOnSampleTabResource("/GSB-32_unaccession.json", sampleTabString -> {
			log.info("POSTing to " + uriSb);
			RequestEntity<String> request = RequestEntity.post(uriSb).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			log.info(""+response.getBody());
			
			if (!response.getBody().contains("SAMEA2186845")) {
				throw new RuntimeException("Response does not have expected accession SAMEA2186845");
			}
		});	
	}

	@Override
	protected void phaseThree() {
		
		log.info("Testing SampleTab JSON submission deleted");
		runCallableOnSampleTabResource("/GSB-32_deleted.json", sampleTabString -> {
			log.info("POSTing to " + uriSb);
			RequestEntity<String> request = RequestEntity.post(uriSb).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			log.info(""+response.getBody());
			
			Optional<Resource<Sample>> clientSample = client.fetchSampleResource("SAMEA2186844");
			if (clientSample.isPresent()) {
				throw new RuntimeException("Found deleted sample SAMEA2186844");
			}
		});	
		
		log.info("Testing SampleTab JSON accession in multiple samples");
		runCallableOnSampleTabResource("/GSB-44_ownership.json", sampleTabString -> {
			log.info(sampleTabString);
			log.info("POSTing to " + uriSb);
			RequestEntity<String> request = RequestEntity.post(uriSb).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			log.info(response.getBody());
			//response is a JSON object of stuff
			//just try to match the error message for now - messy but quick
			if (!response.getBody().contains("was previously described in")) {
				//TODO do this properly once it is all fixed up
				throw new RuntimeException("Unable to recognize duplicate sample");
			}
			log.info(""+response.getBody());
		});	
	}

	@Override
	protected void phaseFour() {
		// Find Sample
//		Filter nameFilter = FilterBuilder.create().onName("JJSample").build();
//		PagedResources<Resource<Sample>> samplePage = client.fetchPagedSampleResource("*:*",
//				Collections.singleton(nameFilter), 0, 1);
//		assert samplePage.getMetadata().getTotalElements() == 1;
//
//		Sample jjSample = samplePage.getContent().iterator().next().getContent();
		//TODO do this better
//		assertThat(jjSample.getContacts(), hasSize(2));
//		assertThat(jjSample.getOrganizations(), hasSize(2));
//		assertThat(jjSample.getPublications(), hasSize(2));
//
//		assertThat(jjSample.getPublications().first().getPubMedId(), notNullValue());

		// Find Group
//		nameFilter  = FilterBuilder.create().onName("JJGroup").build();
//		samplePage = client.fetchPagedSampleResource("*:*",
//				Collections.singleton(nameFilter), 0, 1);
//		assert samplePage.getMetadata().getTotalElements() == 1;
//
//		Sample jjGroup = samplePage.getContent().iterator().next().getContent();
//		assertThat(jjGroup.getContacts(), hasSize(2));
//		assertThat(jjGroup.getOrganizations(), hasSize(2));
//		assertThat(jjGroup.getPublications(), hasSize(2));
//
//		assertThat(jjGroup.getPublications().first().getPubMedId(), notNullValue());

		// Evaluate implicit relationships are converted propertly
		Filter nameFilter = FilterBuilder.create().onName("ValidOrigin").build();
		PagedResources<Resource<Sample>> samplePage = client.fetchPagedSampleResource("*:*",
				Collections.singleton(nameFilter), 0, 1);
		if (samplePage.getMetadata().getTotalElements() != 1) {
			throw new RuntimeException("Unexpected number of samples found with name ValidOrigin");
		}
		Sample validOriginSample = samplePage.getContent().iterator().next().getContent();

		Filter derivedFromFilter = FilterBuilder.create()
				.onRelation("derived from")
				.withValue(validOriginSample.getAccession())
				.build();

		Filter derivedSampleName = FilterBuilder.create()
				.onName("DerivedFromValidOrigin")
				.build();

		samplePage = client.fetchPagedSampleResource("*:*",
				Stream.of(derivedFromFilter, derivedSampleName).collect(Collectors.toList()), 0, 1);


		if (samplePage.getMetadata().getTotalElements() != 1) {
			throw new RuntimeException("Unexpected number of samples found with name DerivedFromValidOrigin and " +
					"relationship");
		}
		Sample derivedFromValidOriginSample = samplePage.getContent().iterator().next().getContent();
		for(Relationship rel: derivedFromValidOriginSample.getRelationships()) {
			if (!rel.getTarget().matches("SAM[END][AG]?[0-9]+")) {
				throw new RuntimeException("Sample contains invalid relationship");
			}
		}
	}

	@Override
	protected void phaseFive() {
		
	}


	private interface SampleTabStringCallback {
		public void callback(String sampleTabString);

	}

	private interface SampleTabFileCallback {
		public void callback(ClassPathResource sampletabFile);
	}

	private void runCallableOnSampleTabResource(String resource, SampleTabStringCallback callback) {
		URL url = Resources.getResource(resource);
		String text = null;
		try {
			text = Resources.toString(url, Charsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			callback.callback(text);			
		}
	}

	private void runCallableOnSampleTabFileResource(String resource, SampleTabFileCallback callback) {
		ClassPathResource classPathResource = new ClassPathResource(resource);
		callback.callback(classPathResource);

	}


}
