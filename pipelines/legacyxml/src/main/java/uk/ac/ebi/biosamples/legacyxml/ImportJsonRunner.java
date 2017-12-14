package uk.ac.ebi.biosamples.legacyxml;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.legacy.json.service.JSONSampleToSampleConverter;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.JsonFragmenter;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static uk.ac.ebi.biosamples.utils.JsonFragmenter.JsonCallback;

@Component
public class ImportJsonRunner implements ApplicationRunner {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final BioSamplesClient client;
	private final JsonFragmenter jsonFragmenter;
	private final JSONSampleToSampleConverter jsonSampleToSampleConverter;

	private static final String DOMAIN="self.BioSamplesMigration";

	public ImportJsonRunner(BioSamplesClient client, JsonFragmenter jsonFragmenter,
                            JSONSampleToSampleConverter jsonSampleToSampleConverter) {
		this.client = client;
		this.jsonFragmenter = jsonFragmenter;
		this.jsonSampleToSampleConverter = jsonSampleToSampleConverter;
	}
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		if (!"import-json".equals(args.getNonOptionArgs().get(0))) {
			return;
		}
		
		Path inputJsonPath = Paths.get(args.getNonOptionArgs().get(1));

		try (InputStream is = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(inputJsonPath)))) {

			Map<String, Future<Resource<Sample>>> futures = new LinkedHashMap<>();

			JsonCallback callback = new ImportJsonCallback(futures, client, jsonSampleToSampleConverter);

			// this does the actual processing
			jsonFragmenter.handleStream(is, "UTF-8", callback);

			log.info("waiting for futures");

			// wait for anything to finish
			ThreadUtils.checkFutures(futures, 0);
		}
	}
	
	
	public static class ImportJsonCallback implements JsonCallback {

		Logger log = LoggerFactory.getLogger(getClass());


		private final Map<String, Future<Resource<Sample>>> futures;
		private final BioSamplesClient client;
		private final JSONSampleToSampleConverter jsonSampleToSampleConverter;

		public ImportJsonCallback(Map<String, Future<Resource<Sample>>> futures, BioSamplesClient client, JSONSampleToSampleConverter jsonSampleToSampleConverter) {
			this.futures = futures;
			this.client = client;
			this.jsonSampleToSampleConverter = jsonSampleToSampleConverter;
		}
		
		@Override
		public void handleJson(String json) throws Exception {
			
			Sample sample = jsonSampleToSampleConverter.convert(json);

			Optional<Resource<Sample>> optionalSampleResource = client.fetchSampleResource(sample.getAccession());
			if (optionalSampleResource.isPresent()) {

			    Sample xmlSample = optionalSampleResource.get().getContent();
//			    SortedSet<Attribute> attributes = mergeAttributes(xmlSample.getAttributes(), sample.getAttributes());
//			    SortedSet<ExternalReference> externalReferences = merge(xmlSample.getExternalReferences(), sample.getExternalReferences());

				// The relationship we consider here are samples part of a group
				SortedSet<Relationship> relationships = merge(xmlSample.getRelationships(), sample.getRelationships());
			    SortedSet<Organization> organizations = merge(xmlSample.getOrganizations(), sample.getOrganizations());
				SortedSet<Contact> contacts = merge(xmlSample.getContacts(), sample.getContacts());
				SortedSet<Publication> publications = merge(xmlSample.getPublications(), sample.getPublications());


			    sample = Sample.build(xmlSample.getName(), xmlSample.getAccession(), DOMAIN,
						xmlSample.getRelease(), xmlSample.getUpdate(),
						xmlSample.getAttributes(), relationships, xmlSample.getExternalReferences(),
						organizations, contacts, publications);



			} else {
                    //need to specify domain
                 sample = Sample.build(sample.getName(), sample.getAccession(), DOMAIN,
                            sample.getRelease(), sample.getUpdate(),
                            new TreeSet<>(), sample.getRelationships(), new TreeSet<>(),
                            sample.getOrganizations(), sample.getContacts(), sample.getPublications());
			}

			futures.put(json, client.persistSampleResourceAsync(sample, false, true));
//            try {
//				log.info("Submitting sample " + objectMapper.writeValueAsString(sample));
//				client.persistSampleResource(sample, false, true);
//			} catch (Exception e) {
//				log.error("An exception occured while submitting sample " + sample.getAccession(), e);
//				throw e;
//			}

			//make sure we don't have too many futures
			ThreadUtils.checkFutures(futures, 100);
		}


		private String camelcaser(String value) {
			StringBuilder finalString = new StringBuilder();
			for(String part: value.split(" ")) {
				part = part.replaceAll("[^A-Za-z0-9]","");
				part = StringUtils.capitalize(part.toLowerCase());
				if (finalString.length() == 0) {
					finalString.append(part.toLowerCase());
					continue;
				}
				finalString.append(part);
			}
			return finalString.toString();
		}

		private SortedSet<Attribute> mergeAttributes(SortedSet<Attribute> original, SortedSet<Attribute> json) {
			SortedSet<Attribute> completeSet = new TreeSet<>(original);
			List<String> orginalAttributeTypesCamelCased = original.stream()
					.map(a ->camelcaser(a.getType())).collect(Collectors.toList());

			completeSet.addAll(
					json.stream()
							.filter(a -> !orginalAttributeTypesCamelCased.contains(a.getType()))
							.collect(Collectors.toList())
			);

			return completeSet;
		}

		private <T> SortedSet<T> merge(SortedSet<T> first, SortedSet<T> second) {
			SortedSet<T> completeSet = new TreeSet<>(first);
			completeSet.addAll(second);
			return completeSet;
		}

	}

	private static class BioSampleAsyncFetcher implements Supplier<Optional<Resource<Sample>>> {

		private final String sampleAccession;
		private final BioSamplesClient client;

		private BioSampleAsyncFetcher(String sampleAccession, BioSamplesClient client) {
			this.sampleAccession = sampleAccession;
			this.client = client;
		}

		@Override
		public Optional<Resource<Sample>> get() {
			return client.fetchSampleResource(sampleAccession);
		}
	}


}
