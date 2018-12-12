package uk.ac.ebi.biosamples.legacyxml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.legacy.json.service.JSONSampleToSampleConverter;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.JsonFragmenter;
import uk.ac.ebi.biosamples.utils.JsonFragmenter.JsonCallback;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

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

			Map<String, Future<Void>> futures = new LinkedHashMap<>();
			ExecutorService executorService = null;
			try  {
				executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, 1, 8);
				JsonCallback callback = new ImportJsonCallback(futures, 
						client, jsonSampleToSampleConverter, executorService);

				// this does the actual processing
				jsonFragmenter.handleStream(is, "UTF-8", callback);

				log.info("waiting for futures");

				// wait for anything to finish
				ThreadUtils.checkFutures(futures, 0);
			} finally {
				if (executorService != null) {
					executorService.shutdownNow();
				}
			}

		}
	}
	
	
	public static class ImportJsonCallback implements JsonCallback {

		Logger log = LoggerFactory.getLogger(getClass());


		private final Map<String, Future<Void>> futures;
		private final BioSamplesClient client;
		private final JSONSampleToSampleConverter jsonSampleToSampleConverter;
		private final ExecutorService executorService;

		public ImportJsonCallback(Map<String, Future<Void>> futures, BioSamplesClient client, 
				JSONSampleToSampleConverter jsonSampleToSampleConverter, ExecutorService executorService) {
			this.futures = futures;
			this.client = client;
			this.jsonSampleToSampleConverter = jsonSampleToSampleConverter;
			this.executorService = executorService;
		}
		
		@Override
		public void handleJson(String json) throws Exception {	

			
			futures.put(json, executorService.submit(new JsonCallable(client, jsonSampleToSampleConverter, json)));
			//make sure we don't have too many futures
			ThreadUtils.checkFutures(futures, 100);
		}

	}
	
	private static class JsonCallable implements Callable<Void> {
		private final BioSamplesClient client;
		private final JSONSampleToSampleConverter jsonSampleToSampleConverter;
		private final String json;

		public JsonCallable(BioSamplesClient client, 
				JSONSampleToSampleConverter jsonSampleToSampleConverter, 
				String json) {
			this.client = client;
			this.jsonSampleToSampleConverter = jsonSampleToSampleConverter;
			this.json = json;
		}
		
		@Override
		public Void call() throws Exception {
			Sample sample = jsonSampleToSampleConverter.convert(json);
			
			if (sample.getRelationships().size() == 0 &&
					sample.getOrganizations().size() == 0 &&
					sample.getContacts().size() == 0 &&
					sample.getPublications().size() == 0) {
				//nothing in the json we want to keep, skip
				return null;
			}

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


//			    sample = Sample.build(xmlSample.getName(), xmlSample.getAccession(), DOMAIN,
//						xmlSample.getRelease(), xmlSample.getUpdate(),
//						xmlSample.getAttributes(), relationships, xmlSample.getExternalReferences(),
//						organizations, contacts, publications);
                sample = Sample.Builder.fromSample(xmlSample).withDomain(DOMAIN)
						.withRelationships(relationships)
						.withOrganizations(organizations).withContacts(contacts).withPublications(publications)
						.build();





			} else {
                    //need to specify domain
//                 sample = Sample.build(sample.getName(), sample.getAccession(), DOMAIN,
//                            sample.getRelease(), sample.getUpdate(),
//                            new TreeSet<>(), sample.getRelationships(), new TreeSet<>(),
//                            sample.getOrganizations(), sample.getContacts(), sample.getPublications());
//                sample = new Sample.Builder(sample.getName(), sample.getAccession()).withDomain(DOMAIN)
//						.withRelease(sample.getRelease()).withUpdate(sample.getUpdate())
//						.withRelationships(sample.getRelationships())
//						.withOrganizations(sample.getOrganizations()).withContacts(sample.getContacts())
//						.withPublications(sample.getPublications()).build();
                sample = Sample.Builder.fromSample(sample).withDomain(DOMAIN)
						.withNoAttributes().withNoExternalReferences().build();
			}
			
			client.persistSampleResource(sample, false, true);
			return null;
		}

		private <T> SortedSet<T> merge(SortedSet<T> first, SortedSet<T> second) {
			SortedSet<T> completeSet = new TreeSet<>(first);
			completeSet.addAll(second);
			return completeSet;
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
		
	}

}
