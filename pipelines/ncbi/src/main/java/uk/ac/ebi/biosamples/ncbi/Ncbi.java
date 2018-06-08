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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.AccessionFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
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
	
	private final AccessionCallback accessionCallback;
	
	public Ncbi(PipelinesProperties pipelinesProperties, 
			XmlFragmenter xmlFragmenter,
			NcbiFragmentCallback sampleCallback, 
			BioSamplesClient bioSamplesClient) {
		this.pipelinesProperties = pipelinesProperties;
		this.xmlFragmenter = xmlFragmenter;
		this.sampleCallback = sampleCallback;
		this.bioSamplesClient = bioSamplesClient;
		this.accessionCallback = new AccessionCallback(pipelinesProperties);		
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
		} else {
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
		log.info("Handled new and updated NCBI samples");
		
		//wait until the last accession is avaliable by search 
		String lastAccession = accessionCallback.getLastAccession();
		waitUntilAccessionSearchable(lastAccession);

		log.debug("Number of accession from NCBI = "+accessionCallback.accessions.size());
		
		//remove old NCBI samples no longer present
		
		Set<String> toRemoveAccessions = getExstingPublicNcbiAccessions();		
		toRemoveAccessions.removeAll(accessionCallback.accessions);		
		log.debug("Number of samples to remove = "+toRemoveAccessions.size());
		
		//remove those samples that are left
		for (String accession : toRemoveAccessions) {
			// this must get the ORIGINAL sample without curation
			Optional<Resource<Sample>> sampleOptional = bioSamplesClient.fetchSampleResource(accession, Optional.empty());
			
			if (sampleOptional.isPresent()) {
				Sample sample = sampleOptional.get().getContent();
				
				//set the release date to 100 years in the future to make it private again
				Sample newSample = Sample.build(sample.getName(), 
						sample.getAccession(), 
						sample.getDomain(), 
						ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant(), 
						sample.getUpdate(), 
						sample.getAttributes(), 
						sample.getRelationships(), 
						sample.getExternalReferences(), 
						sample.getOrganizations(), 
						sample.getContacts(), 
						sample.getPublications());
				
				//persist the now private sample
				log.debug("Making private "+sample.getAccession());
				bioSamplesClient.persistSampleResource(newSample);
			}
		}

		log.info("Processed NCBI pipeline");
	}
	
	private Set<String> getExstingPublicNcbiAccessions() {
		long startTime = System.nanoTime();
		//make sure to only get the public samples
		Set<String> existingAccessions = new TreeSet<>();		
		for (Resource<Sample> sample : bioSamplesClient.getPublicClient().get().fetchSampleResourceAll(
				Collections.singleton(FilterBuilder.create().onAccession("SAM[^E].*").build()))) {
			existingAccessions.add(sample.getContent().getAccession());
		}	
		
		long endTime = System.nanoTime();
		double intervalSec = ((double)(endTime-startTime))/1000000000.0;
		log.debug("Took "+intervalSec+"s to get "+existingAccessions.size()+" existing accessions");	
		return existingAccessions;
	}
	
	private void waitUntilAccessionSearchable(String accession) {
		long startTime = System.nanoTime();
		
		List<Filter> filters = new ArrayList<>();
		filters.add(new AccessionFilter.Builder(accession).build());
		List<Resource<Sample>> samples = new ArrayList<>();
		
		while(samples.size() == 0) {
			samples = new ArrayList<>();
			for (Resource<Sample> sample : bioSamplesClient.fetchSampleResourceAll(filters)) {
				samples.add(sample);
			}
			//wait for a minute before trying again
			try {
				Thread.sleep(1000*60);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		
		long endTime = System.nanoTime();
		double intervalSec = ((double)(endTime-startTime))/1000000000.0;
		log.debug("Took "+intervalSec+"s to wait for last accession "+accession);
	}
	
	private static class AccessionCallback implements ElementCallback {
		
		public SortedSet<String> accessions = new TreeSet<>();
		private final PipelinesProperties pipelinesProperties;
		
		private String lastAccession = null;

		public AccessionCallback(PipelinesProperties pipelinesProperties) {
			this.pipelinesProperties = pipelinesProperties;
		}
		
		public String getLastAccession() {
			return lastAccession;
		}
		
		@Override
		public void handleElement(Element e) throws Exception {
			
			//TODO check status
			
			String accession = e.attributeValue("accession");
			accessions.add(accession);
			lastAccession = accession;
		}

		@Override
		public boolean isBlockStart(String uri, String localName, String qName, Attributes attributes) {
			//its not a biosample element, skip
			if (!qName.equals("BioSample")) {
				return false;
			}
			//its not public, skip
			if (attributes.getValue("", "access").equals("public")) {
				//do nothing
			} else if (pipelinesProperties.getNcbiControlledAccess() && 
					attributes.getValue("", "access").equals("controlled-access")) {
				//do nothing
			} else {
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
