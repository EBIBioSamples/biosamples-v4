package uk.ac.ebi.biosamples.legacyxml;

import com.opencsv.CSVReader;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.XmlGroupToSampleConverter;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.XmlFragmenter;
import uk.ac.ebi.biosamples.utils.XmlFragmenter.ElementCallback;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

@Component
public class ImportRunner implements ApplicationRunner {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final BioSamplesClient client;
	private final XmlFragmenter xmlFragmenter;
	private final XmlSampleToSampleConverter xmlSampleToSampleConverter;
	private final XmlGroupToSampleConverter xmlGroupToSampleConverter;
	
	private static final String DOMAIN="self.BioSamplesMigration";
	
	public ImportRunner(BioSamplesClient client, XmlFragmenter xmlFragmenter, 
			XmlSampleToSampleConverter xmlSampleToSampleConverter, XmlGroupToSampleConverter xmlGroupToSampleConverter) {
		this.client = client;
		this.xmlFragmenter = xmlFragmenter;
		this.xmlSampleToSampleConverter = xmlSampleToSampleConverter;
		this.xmlGroupToSampleConverter = xmlGroupToSampleConverter;
	}
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		if (!"import".equals(args.getNonOptionArgs().get(0))) {
			return;
		}
		
		Path inputXmlPath = Paths.get(args.getNonOptionArgs().get(1));
		Path inputCsvPath = Paths.get(args.getNonOptionArgs().get(2));
		
		Path membershipCsv = Paths.get(inputCsvPath.toString(), "membership.csv");
		log.info("Reading membership from "+membershipCsv);
		
		Path sameAsCsv = Paths.get(inputCsvPath.toString(), "sameas.csv");
		log.info("Reading same as from "+sameAsCsv);
		
		Path childOfCsv = Paths.get(inputCsvPath.toString(), "childof.csv");
		log.info("Reading child of from "+childOfCsv);
		
		Path recuratedFromCsv = Paths.get(inputCsvPath.toString(), "recuratedfrom.csv");
		log.info("Reading recurated from from "+recuratedFromCsv);
		
		
		Map<String, Set<String>> groupMembership = reverse(readCsv(membershipCsv));
		Map<String, Set<String>> sameAs = readCsv(sameAsCsv);
		Map<String, Set<String>> childOf = readCsv(childOfCsv);
		Map<String, Set<String>> recuratedFrom = readCsv(recuratedFromCsv);
		
		try (InputStream is = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(inputXmlPath)))) {

			Map<Element, Future<Resource<Sample>>> futures = new LinkedHashMap<>();

			ElementCallback callback = new ImportElementCallback(futures, 
					client, xmlSampleToSampleConverter, xmlGroupToSampleConverter, groupMembership, sameAs, childOf, recuratedFrom);

			// this does the actual processing
			xmlFragmenter.handleStream(is, "UTF-8", callback);

			log.info("waiting for futures");

			// wait for anything to finish
			ThreadUtils.checkFutures(futures, 0);
		}
	}
	
	
	private Map<String,Set<String>> readCsv(Path path) throws IOException {
		Map<String, Set<String>> groupMembership = new HashMap<>();
		try (CSVReader csvReader = new CSVReader(Files.newBufferedReader(path))) {
			String[] nextRecord;
            while ((nextRecord = csvReader.readNext()) != null) {
            	String group = nextRecord[0];
            	for (int i = 1; i < nextRecord.length; i++) {
            		if (!groupMembership.containsKey(group)) {
            			groupMembership.put(group, new HashSet<>());
            		}
            		groupMembership.get(group).add(nextRecord[i]);
            	}
            }
		}
		return groupMembership;
	}
	
	private Map<String, Set<String>> reverse(Map<String, Set<String>> input) {
		Map<String, Set<String>> output = new HashMap<>();
		for (String key : input.keySet()) {
			for (String value : input.get(key)) {
				if (!output.containsKey(value)) {
					output.put(value, new HashSet<>());
				}
				output.get(value).add(key);
			}
		}
		return output;
	}
	
	public static class ImportElementCallback implements ElementCallback {

		
		private final Map<Element, Future<Resource<Sample>>> futures;
		private final BioSamplesClient client;
		private final XmlSampleToSampleConverter xmlSampleToSampleConverter;
		private final XmlGroupToSampleConverter xmlGroupToSampleConverter;
		private final Map<String, Set<String>> groupMembership;
		private final Map<String, Set<String>> sameAs;
		private final Map<String, Set<String>> childOf;
		private final Map<String, Set<String>> recuratedFrom;
		
		public ImportElementCallback(Map<Element, Future<Resource<Sample>>> futures, BioSamplesClient client, 
				XmlSampleToSampleConverter xmlSampleToSampleConverter, XmlGroupToSampleConverter xmlGroupToSampleConverter,
				Map<String, Set<String>> groupMembership, Map<String, Set<String>> sameAs, Map<String, Set<String>> childOf,
				Map<String, Set<String>> recuratedFrom) {
			this.futures = futures;
			this.client = client;
			this.xmlSampleToSampleConverter = xmlSampleToSampleConverter;
			this.xmlGroupToSampleConverter = xmlGroupToSampleConverter;
			this.groupMembership = groupMembership;
			this.sameAs = sameAs;
			this.childOf = childOf;
			this.recuratedFrom = recuratedFrom;
		}
		
		@Override
		public void handleElement(Element e) throws Exception {
			
			Sample sample;
			if (e.getName().equals("BioSample")) {
				sample = xmlSampleToSampleConverter.convert(e);
			} else if (e.getName().equals("BioSampleGroup")) {
				sample = xmlGroupToSampleConverter.convert(e);
			} else {
				return;
			}

			//need to specify domain
//			sample = Sample.build(sample.getName(), sample.getAccession(), DOMAIN,
//					sample.getRelease(), sample.getUpdate(),
//					sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences(),
//					sample.getOrganizations(), sample.getContacts(), sample.getPublications());
            sample = Sample.Builder.fromSample(sample).withDomain(DOMAIN).build();
			
			//need to add "has member" relationships
			if (groupMembership.containsKey(sample.getAccession())) {
				for (String target : groupMembership.get(sample.getAccession())) {
					Relationship r = Relationship.build(sample.getAccession(), "has member", target);
					//TODO access this in an immutable manner
					sample.getRelationships().add(r);
				}
			}
			/*
			if (sameAs.containsKey(sample.getAccession())) {
				for (String target : sameAs.get(sample.getAccession())) {
					Relationship r = Relationship.build(sample.getAccession(), "same as", target);
					//TODO access this in an immutable manner
					sample.getRelationships().add(r);
				}
			}
			if (childOf.containsKey(sample.getAccession())) {
				for (String target : childOf.get(sample.getAccession())) {
					Relationship r = Relationship.build(sample.getAccession(), "child of", target);
					//TODO access this in an immutable manner
					sample.getRelationships().add(r);
				}
			}
			if (recuratedFrom.containsKey(sample.getAccession())) {
				for (String target : recuratedFrom.get(sample.getAccession())) {
					Relationship r = Relationship.build(sample.getAccession(), "recurated from", target);
					//TODO access this in an immutable manner
					sample.getRelationships().add(r);
				}
			}
			*/
			
			futures.put(e, client.persistSampleResourceAsync(sample, false, true));

			//make sure we don't have too many futures
			ThreadUtils.checkFutures(futures, 100);
		}

		@Override
		public boolean isBlockStart(String uri, String localName, String qName, Attributes attributes) {
			return qName.equals("BioSample");
		}
		
	}

}
