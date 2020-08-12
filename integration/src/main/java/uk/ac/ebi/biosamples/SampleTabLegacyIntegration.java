package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.service.SampleUtils;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@Component
public class SampleTabLegacyIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final RestOperations restTemplate;
	private final SampleUtils sampleUtils;


	//	private final URI uriSb;
//	private final URI uriVa;
//	private final URI uriAc;
	private final URI fileUriSb;
	private final URI fileUriAc;
	private final URI fileUriVa;

	public SampleTabLegacyIntegration(RestTemplateBuilder restTemplateBuilder, IntegrationProperties integrationProperties, BioSamplesClient client) {
        super(client);
		this.restTemplate = restTemplateBuilder.build();

		sampleUtils = new SampleUtils();
		Pattern accessionPattern = Pattern.compile(sampleUtils.getAccessionPattern(), Pattern.MULTILINE);

//		uriVa = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
//				.pathSegment("api", "v1", "json", "va").build().toUri();
//
//		uriAc = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
//				.pathSegment("api", "v1", "json", "ac")
//				.queryParam("apikey", integrationProperties.getLegacyApiKey())
//				.build().toUri();
//
//		uriSb = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
//				.pathSegment("api", "v1", "json", "sb")
//				.queryParam("apikey", integrationProperties.getLegacyApiKey())
//				.build().toUri();

		fileUriAc = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
				.pathSegment("api", "v1", "file", "ac")
				.queryParam("apikey", integrationProperties.getLegacyApiKey())
				.build().toUri();

		fileUriSb = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
				.pathSegment("api", "v1", "file", "sb")
				.queryParam("apikey", integrationProperties.getLegacyApiKey())
				.build().toUri();

		fileUriVa = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
				.pathSegment("api", "v1", "file", "va")
				.queryParam("apikey", integrationProperties.getLegacyApiKey())
				.build().toUri();


	}

	@Override
	protected void phaseOne() {

//		TODO: 2018/10/04 - REMOVE JSON ENDPOINT
//		log.info("Testing SampleTab JSON validation");
//		runCallableOnSampleTabResource("/GSB-32.json", sampleTabString -> {
//			log.info("POSTing to " + uriVa);
//			RequestEntity<String> request = RequestEntity.post(uriVa).contentType(MediaType.APPLICATION_JSON)
//					.body(sampleTabString);
//			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
//			log.info(""+response.getBody());
//		});
        log.info("Testing SampleTab File validation");
		runCallableOnSampleTabFile("/GSB-32.txt", sampletabFile -> {
			ResponseEntity<String> response = validateSampletabFile(sampletabFile);
			log.info(""+response.getBody());

		});

//		TODO: 2018/10/04 - REMOVE JSON ENDPOINT
//		log.info("Testing SampleTab JSON accession");
//		runCallableOnSampleTabResource("/GSB-32.json", sampleTabString -> {
//			log.info("POSTing to " + uriAc);
//			RequestEntity<String> request = RequestEntity.post(uriAc).contentType(MediaType.APPLICATION_JSON)
//					.body(sampleTabString);
//			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
//			log.info(""+response.getBody());
//
//			if (!response.getBody().contains("SAMEA2186845")) {
//				throw new RuntimeException("Response does not have expected accession SAMEA2186845");
//			}
//		});
		log.info("Testing SampleTab File accession");
		runCallableOnSampleTabFile("/GSB-32.txt", sampletabFile -> {
			ResponseEntity<String> response = accessionSampletabFile(sampletabFile);
			log.info(""+response.getBody());

		});

		// Can't submit this now because the agent didnt run yet and will return an ownership error
//		log.info("Testing SampleTab JSON submission");
//		runCallableOnSampleTabResource("/GSB-32.json", sampleTabString -> {
//			log.info("POSTing to " + uriSb);
//			RequestEntity<String> request = RequestEntity.post(uriSb).contentType(MediaType.APPLICATION_JSON)
//					.body(sampleTabString);
//			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
//			log.info(""+response.getBody());
//
//			if (!response.getBody().contains("SAMEA2186845")) {
//				throw new RuntimeException("Response does not have expected accession SAMEA2186845");
//			}
//		});

//		TODO: 2018/10/04 - REMOVE JSON ENDPOINT
//		log.info("Testing unaccessioned SampleTab JSON submission ");
//		runCallableOnSampleTabResource("/GSB-new.json", sampleTabString -> {
//			log.info("POSTing to " + uriSb);
//			RequestEntity<String> request = RequestEntity.post(uriSb).contentType(MediaType.APPLICATION_JSON)
//					.body(sampleTabString);
//			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
//			//TODO do this check better
//			if (!response.getBody().contains("GSB-")) {
//				throw new RuntimeException("Response does not have expected submission identifier");
//			}
//			log.info(""+response.getBody());
//		});
		log.info("Testing unaccessioned SampleTab File submission ");
		runCallableOnSampleTabFile("/GSB-new.txt", sampletabFile -> {
			ResponseEntity<String> response = submitSampletabFile(sampletabFile);
			log.info(""+response.getBody());
		});

		log.info("Testing submission of sampletab with valid implicit relationship");
		runCallableOnSampleTabFile("/Implicit_relationship_sampletab.tab", sampletabFile -> {
			ResponseEntity<String> response = submitSampletabFile(sampletabFile);
			if (!response.getBody().contains("GSB-")) {
				log.error(response.toString());
				throw new RuntimeException("Response does not have expected submission identifier");
			}
			log.info(""+response.getBody());
		});

		log.info("Testing pre-accessioning of SampleTab with valid implicit relationship");
		runCallableOnSampleTabFile("/accessioned_sampletab_with_implicit_relationships.tab", sampletabFile -> {
			ResponseEntity<String> response = accessionSampletabFile(sampletabFile);
            Stream.of("SAMEA7231988", "SAMEA7231987", "SAMEA7231986", "SAMEA7231985")
			.forEach(acc -> {
				if (!response.getBody().contains(acc)) {
					throw new RuntimeException("Accessioned sampletab does not contains expected accession " + acc);
				}
			});//			Pattern accessionPattern = Pattern.compile("(SAMEA\\d+)");
//			Matcher m = accessionPattern.matcher(response.getBody());
//			Set<String> accessionToFind= Stream.of("SAMEA7231988", "SAMEA7231987", "SAMEA7231986", "SAMEA7231985").collect(Collectors.toSet());
//			while(m.find()) {
//				if (!accessionToFind.contains(m.group())) {
////					throw new RuntimeException("Found accession " + m.group() + " not part of the pre-accession sampletabs");
//				} else {
//					accessionToFind.remove(m.group());
//				}
//			}
//			if (accessionToFind.size() > 0) {
//				String accessionsNotFound = accessionToFind.stream().reduce("", (s, s2) -> s +" ," + s2);
//				throw new RuntimeException("Response does not have expected accessions: " + accessionsNotFound);
//			}
		});

		log.info("Testing rejection submission of sampletab with invalid implicit relationship");
		runCallableOnSampleTabFile("/Invalid_implicit_relationship_sampletab.txt", sampletabFile -> {
			ResponseEntity<String> response = submitSampletabFile(sampletabFile);
			if (!response.getBody().contains("Unable to accession")) {
				log.error(response.toString());
				throw new RuntimeException("Response does not have expected submission identifier");
			}
			log.info("SampleTab with invalid relation has been rejected as expected");
			log.info(""+response.getBody());
		});

		log.info("Testing SampleTab file submission with DatabaseURI");
		runCallableOnSampleTabFile("/GSB-52.txt", sampletabFile -> {
			ResponseEntity<String> response = submitSampletabFile(sampletabFile);

			if (!response.getBody().contains("GSB-")) {
				log.error(response.toString());
				throw new RuntimeException("Response does not have expected submission identifier");
			}
			log.info("SampleTab with invalid relation has been rejected as expected");
			log.info(""+response.getBody());

		});

		log.info("Testing SampleTab file submission with new line character");
		runCallableOnSampleTabFile("/sampletab_with_escaped_newline.txt", sampletabFile -> {
			ResponseEntity<String> response = submitSampletabFile(sampletabFile);

			if (!response.getBody().contains("GSB-")) {
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

		Filter nameFilter = FilterBuilder.create().onName("foozitTest 1").build();
		Iterator<Resource<Sample>> sampleResourceIterator = client.fetchSampleResourceAll(null, Collections.singletonList(nameFilter)).iterator();
		if (!sampleResourceIterator.hasNext()) {
			throw new RuntimeException("Unable to find submitted sample with name foozitTest 1");
		}
		Sample sample = sampleResourceIterator.next().getContent();
		if (sampleResourceIterator.hasNext()) {
			throw new RuntimeException("More than one sample found with name foozitTest 1");
		}

		if (sample.getRelationships().size() == 0) {
			throw new RuntimeException("Sample foozitTest 1 should have some relationship but it doesn't");
		} else {
			boolean inGroup = false;
			String sampleAccession = sample.getAccession();
			for (Relationship r : sample.getRelationships()) {
				if (r.getTarget().equals(sampleAccession) && r.getType().equals("has member")) {
					inGroup = true;
					if (!r.getSource().startsWith("SAMEG")) {
						throw new RuntimeException("foozitTest 1 has 'has member' relationship to something not SAMEG");
					}
				}
				//check each relationship has a sane source and target
				if (!r.getTarget().matches(sampleUtils.getAccessionPattern())) {
					throw new RuntimeException("foozitTest 1 has relationship to "+r.getTarget());
				}
				if (!r.getSource().matches(sampleUtils.getAccessionPattern())) {
					throw new RuntimeException("foozitTest 1 has relationship from "+r.getSource());
				}
			}
			if (!inGroup) {
				throw new RuntimeException("foozitTest 1 has no 'has member' relationship to it");
			}
		}


		Optional<List<Attribute>> submissionIdAttribute = sampleUtils.getAttributesWithType(sample, "Submission identifier");
		if (!submissionIdAttribute.isPresent()) {
			throw new RuntimeException("A 'Submission identifier' field should be present on a sample submitted using SampleTab");
		}

		if (submissionIdAttribute.get().size() != 1) {
			throw new RuntimeException("The number of 'Submission identifier' attributes for the sample is not 1");
		}

		// Get all the accession and verify that we are
		String submissionId = submissionIdAttribute.get().get(0).getValue();
		Filter submissionIdFilter = FilterBuilder.create().onAttribute("Submission identifier").withValue(submissionId).build();
		Iterable<Resource<Sample>> sampleResourceAll = client.fetchSampleResourceAll(null, Collections.singletonList(submissionIdFilter));
		List<String> submissionAccessions = StreamSupport.stream(sampleResourceAll.spliterator(), false)
				.map(resource -> resource.getContent().getAccession())
				.filter(accession -> !accession.startsWith("SAMEG"))
				.collect(Collectors.toList());

	}

	@Override
	protected void phaseThree() {

		log.info("Testing submission of pre-accessioned SampleTab with valid implicit relationship");
		runCallableOnSampleTabFile("/accessioned_sampletab_with_implicit_relationships.tab", sampletabFile -> {
			ResponseEntity<String> response = submitSampletabFile(sampletabFile);
			// Check the returned sampletab contains the expected accessions
			Stream.of("SAMEA7231988", "SAMEA7231987", "SAMEA7231986", "SAMEA7231985")
					.forEach(str -> {
                        if (!response.getBody().contains(str)) {
                            log.error(response.toString());
                            throw new RuntimeException("Response does not have expected accession");
                        }
					});
			if (!response.getBody().contains("GSB-")) {
				log.error(response.toString());
				throw new RuntimeException("Response does not contains a valid submission identifier");
			}
		});

//		TODO: 2018/10/04 - REMOVE JSON ENDPOINT
//		log.info("Testing SampleTab JSON with pre-acessioned samples");
//		runCallableOnSampleTabResource("/GSB-32.txt", sampleTabString -> {
//
//			log.info("POSTing to " + uriSb);
//			RequestEntity<String> request = RequestEntity.post(uriSb).contentType(MediaType.APPLICATION_JSON)
//					.body(sampleTabString);
//			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
//			log.info(""+response.getBody());
//
//			Optional<Resource<Sample>> clientSample = client.fetchSampleResource("SAMEA2186844");
//			if (!clientSample.isPresent()) {
//				throw new RuntimeException("Couldn't find pre-accessioned sample SAMEA2186844 after submission");
//			}
//		});
		log.info("Testing SampleTab file with pre-acessioned samples");
		runCallableOnSampleTabFile("/GSB-32.txt", sampletabFile -> {
			ResponseEntity<String> response = submitSampletabFile(sampletabFile);
			log.info(""+response.getBody());

			Optional<Resource<Sample>> clientSample = client.fetchSampleResource("SAMEA2186844");
			if (!clientSample.isPresent()) {
				throw new RuntimeException("Couldn't find pre-accessioned sample SAMEA2186844 after submission");
			}
		});


//		TODO: 2018/10/04 - REMOVE JSON ENDPOINT
//		log.info("Testing SampleTab JSON submission deleted");
//		runCallableOnSampleTabResource("/GSB-32_deleted.json", sampleTabString -> {
//			log.info("POSTing to " + uriSb);
//			RequestEntity<String> request = RequestEntity.post(uriSb).contentType(MediaType.APPLICATION_JSON)
//					.body(sampleTabString);
//			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
//			log.info(""+response.getBody());
//
//			Optional<Resource<Sample>> clientSample = client.fetchSampleResource("SAMEA2186844");
//			if (clientSample.isPresent()) {
//				throw new RuntimeException("Found deleted sample SAMEA2186844");
//			}
//		});

		//TODO: Check why this test fails after Organism is made mandatory
		/*log.info("Testing SampleTab file submission deleted");
		runCallableOnSampleTabFile("/GSB-32_deleted.txt", sampleTabFile -> {
			ResponseEntity<String> response = submitSampletabFile(sampleTabFile);
			log.info(""+response.getBody());

			Optional<Resource<Sample>> clientSample = client.fetchSampleResource("SAMEA2186844");
			if (clientSample.isPresent()) {
				throw new RuntimeException("Found deleted sample SAMEA2186844");
			}
		});*/

//		TODO: 2018/10/04 - REMOVE JSON ENDPOINT
//		log.info("Testing SampleTab JSON accession in multiple samples");
//		runCallableOnSampleTabResource("/GSB-44_ownership.json", sampleTabString -> {
//			log.info(sampleTabString);
//			log.info("POSTing to " + uriSb);
//			RequestEntity<String> request = RequestEntity.post(uriSb).contentType(MediaType.APPLICATION_JSON)
//					.body(sampleTabString);
//			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
//			log.info(response.getBody());
//			//response is a JSON object of stuff
//			//just try to match the error message for now - messy but quick
//			if (!response.getBody().contains("was previously described in")) {
//				//TODO do this properly once it is all fixed up
//				throw new RuntimeException("Unable to recognize duplicate sample");
//			}
//			log.info(""+response.getBody());
//		});

		log.info("Testing SampleTab file accession in multiple samples");
		runCallableOnSampleTabFile("/GSB-44_ownership.txt", sampletabFile -> {
			ResponseEntity<String> response = submitSampletabFile(sampletabFile);
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

		verify_external_references_was_picked_up_from_sampletab();

		verify_implicit_relationships_in_unaccessioned_sampletab_are_converted_and_exported();

		verify_relationships_in_accessioned_sampletab_are_exported_correctly();

		verify_samples_with_escaped_newline_characters_are_kept_the_same();
	}



	@Override
	protected void phaseFive() {

	}



//	private interface SampleTabStringCallback {
//		public void callback(String sampleTabString);
//	}

//	private void runCallableOnSampleTabResource(String resource, SampleTabStringCallback callback) {
//		URL url = Resources.getResource(resource);
//		String text = null;
//		try {
//			text = Resources.toString(url, Charsets.UTF_8);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		} finally {
//			callback.callback(text);
//		}
//	}

    private interface SampleTabFileCallback {
        void callback(ClassPathResource sampletabFile);
    }

	private void runCallableOnSampleTabFile(String resource, SampleTabFileCallback callback) {
	    callback.callback(new ClassPathResource(resource));
	}

	private ResponseEntity<String> postFileToUrl(ClassPathResource resource, URI uri) {
		log.info("POSTing to " + uri);
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("file", resource);

		RequestEntity<LinkedMultiValueMap> request = RequestEntity.post(uri).contentType(MediaType.MULTIPART_FORM_DATA) .body(map);

		return restTemplate.exchange(request, String.class);

	}

	private ResponseEntity<String> submitSampletabFile(ClassPathResource resource) {
		return postFileToUrl(resource, fileUriSb);
	}

	private ResponseEntity<String> accessionSampletabFile(ClassPathResource resource) {
		return postFileToUrl(resource, fileUriAc);
	}

	private ResponseEntity<String> validateSampletabFile(ClassPathResource resource) {
		return postFileToUrl(resource, fileUriVa);
	}

	private void verify_relationships_in_accessioned_sampletab_are_exported_correctly() {
		PagedResources<Resource<Sample>> samplePage;

		Filter nameFilter = FilterBuilder.create()
                .onName("Accessioned ValidOrigin")
				.build();

		samplePage = client.fetchPagedSampleResource("*:*",
				Collections.singletonList(nameFilter), 0, 10);

		if (samplePage.getMetadata().getTotalElements() != 1) {
			throw new RuntimeException("More than one element found with name 'Accessione ValidOrigin'");
		}

		String submissionIdentifier = samplePage.getContent().stream().findFirst().get().getContent()
				.getAttributes().stream().filter(attr -> attr.getType().equalsIgnoreCase("submission identifier"))
				.findFirst().get().getValue();
        Filter attrFilter = FilterBuilder.create()
				.onAttribute("Submission identifier")
				.withValue(submissionIdentifier)
				.build();

		samplePage = client.fetchPagedSampleResource("*:*",
				Collections.singletonList(attrFilter), 0, 10);

		if (samplePage.getMetadata().getTotalElements() != 5) {
			throw new RuntimeException("Unexpected number of samples found with Submission identifier GSB-9191");
		}

		samplePage.getContent().forEach(_sample -> {
			for(Relationship rel: _sample.getContent().getRelationships()) {
				if (!rel.getTarget().matches(sampleUtils.getAccessionPattern())) {
					throw new RuntimeException("Sample "+ _sample.getContent().getName()+ " contains invalid relationship");
				}
			}
		});
	}

	private void verify_implicit_relationships_in_unaccessioned_sampletab_are_converted_and_exported() {
		Filter nameFilter;
		PagedResources<Resource<Sample>> samplePage;

		// Evaluate implicit relationships are converted propertly
		nameFilter = FilterBuilder.create().onName("ValidOrigin").build();
		samplePage = client.fetchPagedSampleResource("*:*",
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
			if (!rel.getTarget().matches(sampleUtils.getAccessionPattern())) {
				throw new RuntimeException("Sample contains invalid relationship");
			}
		}
	}

	private void verify_external_references_was_picked_up_from_sampletab() {
		String sampleNameToCheck = "testExternalRefSample";
		Filter nameFilter = FilterBuilder.create().onName(sampleNameToCheck).build();
		PagedResources<Resource<Sample>> samplePage = client.fetchPagedSampleResource("*:*",
				Collections.singleton(nameFilter), 0, 1);


		Optional<Resource<Sample>> sample = samplePage.getContent().stream().findFirst();
		if (sample.isEmpty()) {
			throw new RuntimeException("Can't find sample submitted using sampletab GSB-52.txt");
		}

		List<String> externalReferencesURL = sample.get().getContent().getExternalReferences()
				.stream().map(ExternalReference::getUrl).collect(Collectors.toList());
		if (!externalReferencesURL.contains("https://hpscreg.eu/cell-line/CENSOi007-A")) {
			throw new RuntimeException("Sample " + sampleNameToCheck + " does not present the expected " +
					"external relationships");
		}
	}

	private void verify_samples_with_escaped_newline_characters_are_kept_the_same() {
	    String sampleNameToCheck = "EscapedNewlineSample";
	    Filter nameFilter = FilterBuilder.create().onName(sampleNameToCheck).build();
	    PagedResources samplePage = client.fetchPagedSampleResource("*:*", Collections.singletonList(nameFilter),
				0, 1);

	    Optional<Resource<Sample>> optionalSampleResource = samplePage.getContent().stream().findFirst();
		if (optionalSampleResource.isEmpty()) {
			throw new RuntimeException("Can't find sample submitted using sampletab 'sampletab_with_escaped_newline.txt'");
		}

		Sample sample = optionalSampleResource.get().getContent();
		boolean descriptionCorrect = false;
		boolean diseaseCorrect = false;
		for (Attribute attr : sample.getCharacteristics()) {
			if (attr.getType().equalsIgnoreCase("description")) {
				descriptionCorrect = attr.getValue().equals("This sample has and escaped newline character \\n inside the description");
			} else if (attr.getType().equalsIgnoreCase("disease")) {
				diseaseCorrect = attr.getValue().equals("Some\\nDisease");
			}
		}

		if (!descriptionCorrect) {
			throw new RuntimeException("Description with newline escaped character is not found correctly");
		}

		if (!diseaseCorrect) {
			throw new RuntimeException("Disease attribute with newline escaped character is not found correctly");
		}

	}
}
