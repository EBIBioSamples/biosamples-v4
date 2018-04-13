package uk.ac.ebi.biosamples.ncbi;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.XmlFragmenter;
import uk.ac.ebi.biosamples.utils.XmlFragmenter.ElementCallback;

@Component
public class Ncbi implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final PipelinesProperties pipelinesProperties;

	private final XmlFragmenter xmlFragmenter;

	private final NcbiFragmentCallback sampleCallback;
	
	private final BioSamplesClient bioSamplesClient;
	
	private final AccessionCallback accessionCallback = new AccessionCallback();
	
	public Ncbi(PipelinesProperties pipelinesProperties, 
			XmlFragmenter xmlFragmenter,
			NcbiFragmentCallback sampleCallback, 
			BioSamplesClient bioSamplesClient) {
		this.pipelinesProperties = pipelinesProperties;
		this.xmlFragmenter = xmlFragmenter;
		this.sampleCallback = sampleCallback;
		this.bioSamplesClient = bioSamplesClient;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Processing NCBI pipeline...");

		LocalDate fromDate = null;
		if (args.getOptionNames().contains("from")) {
			fromDate = LocalDate.parse(args.getOptionValues("from").iterator().next(),
					DateTimeFormatter.ISO_LOCAL_DATE);
		} else {
			fromDate = LocalDate.parse("1000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
		}
		LocalDate toDate = null;
		if (args.getOptionNames().contains("until")) {
			toDate = LocalDate.parse(args.getOptionValues("until").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
		}else {
			toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
		}

		log.info("Processing samples from "+DateTimeFormatter.ISO_LOCAL_DATE.format(fromDate));
		log.info("Processing samples to "+DateTimeFormatter.ISO_LOCAL_DATE.format(toDate));
		sampleCallback.setFromDate(fromDate);
		sampleCallback.setToDate(toDate);

		Path inputPath = Paths.get(pipelinesProperties.getNcbiFile());
		inputPath = inputPath.toAbsolutePath();
		
		try (InputStream is = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(inputPath)))) {

			if (pipelinesProperties.getThreadCount() > 0) {
				ExecutorService executorService = null;
				try {
					executorService = AdaptiveThreadPoolExecutor.create(100, 10000, true, 
							pipelinesProperties.getThreadCount(), pipelinesProperties.getThreadCountMax());
					Map<Element, Future<Void>> futures = new LinkedHashMap<>();

					sampleCallback.setExecutorService(executorService);
					sampleCallback.setFutures(futures);

					// this does the actual processing
					xmlFragmenter.handleStream(is, "UTF-8", sampleCallback, accessionCallback);

					log.info("waiting for futures");

					// wait for anything to finish
					ThreadUtils.checkFutures(futures, 0);
				} finally {
					log.info("shutting down");
					executorService.shutdown();
					executorService.awaitTermination(1, TimeUnit.MINUTES);
				}
			} else {
				// do all on master thread
				// this does the actual processing
				xmlFragmenter.handleStream(is, "UTF-8", sampleCallback);
			}
		}
		log.info("Imported NCBI samples");
		

		log.debug("Number of accession from NCBI = "+accessionCallback.accessions.size());
		
		//TODO remove old NCBI samples no longer present
		Set<String> toRemoveAccessions = new TreeSet<>();
		for (Resource<Sample> sample : bioSamplesClient.fetchSampleResourceAll(
				Collections.singleton(FilterBuilder.create().onAccession("SAM[^E].*").build()))) {
			toRemoveAccessions.add(sample.getContent().getAccession());
		}		

		log.debug("Number of accession in API = "+toRemoveAccessions.size());
		
		toRemoveAccessions.removeAll(accessionCallback.accessions);
		
		log.debug("Number of samples to remove = "+toRemoveAccessions.size());
		
		//remove those samples that are left
		for (String accession : toRemoveAccessions) {
			//TODO this must get the ORIGINAL sample without curation
			Optional<Resource<Sample>> sampleOptional = bioSamplesClient.fetchSampleResource(accession);
			if (sampleOptional.isPresent()) {
				Sample sample = sampleOptional.get().getContent();
				
				//set the release date to 100 years in the future to make it private again
				Sample newSample = Sample.build(sample.getName(), 
						sample.getAccession(), 
						sample.getDomain(), 
						ZonedDateTime.now(ZoneOffset.UTC).plusYears(100).toInstant(), 
						sample.getUpdate(), 
						sample.getAttributes(), 
						sample.getRelationships(), 
						sample.getExternalReferences(), 
						sample.getOrganizations(), 
						sample.getContacts(), 
						sample.getPublications());
				
				//persist the now private sample
				bioSamplesClient.persistSampleResource(newSample);
			}
		}

		log.info("Processed NCBI pipeline");
	}
	
	private static class AccessionCallback implements ElementCallback {
		
		public SortedSet<String> accessions = new TreeSet<>();

		@Override
		public void handleElement(Element e) throws Exception {
			String accession = e.attributeValue("accession");
			accessions.add(accession);
		}

		@Override
		public boolean isBlockStart(String uri, String localName, String qName, Attributes attributes) {
			//its not a biosample element, skip
			if (!qName.equals("BioSample")) {
				return false;
			}
			//its not public, skip
			if (!attributes.getValue("", "access").equals("public")) {
				return false;
			}
			//its an EBI biosample, or has no accession, skip
			if (attributes.getValue("", "accession") == null || attributes.getValue("", "accession").startsWith("SAME")) {
				return false;
			}
			
			//hasn't failed, so we must be interested in it
			return true;
		}
		
	}

}
