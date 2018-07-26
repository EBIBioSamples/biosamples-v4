package uk.ac.ebi.biosamples.ncbi;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.XmlFragmenter;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

@Component
public class Ncbi implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final PipelinesProperties pipelinesProperties;

	private final XmlFragmenter xmlFragmenter;

	private final NcbiFragmentCallback sampleCallback;
	
	private final BioSamplesClient bioSamplesClient;
	
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
					xmlFragmenter.handleStream(is, "UTF-8", sampleCallback);

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

		log.debug("Number of accession from NCBI = "+sampleCallback.getAccessions().size());
		
		//remove old NCBI samples no longer present	
		
		//get all existing NCBI samples
		Set<String> toRemoveAccessions = getExstingPublicNcbiAccessions();
		//remove those that still exist
		toRemoveAccessions.removeAll(sampleCallback.getAccessions());		
		
		//remove those samples that are left
		log.debug("Number of samples to remove = "+toRemoveAccessions.size());
		makePrivate(toRemoveAccessions);

		log.info("Processed NCBI pipeline");
	}
	
	private Set<String> getExstingPublicNcbiAccessions() {
		log.info("getting existing public ncbi accessions");
		long startTime = System.nanoTime();
		//make sure to only get the public samples
		Set<String> existingAccessions = new TreeSet<>();		
		for (Resource<Sample> sample : bioSamplesClient.getPublicClient().get().fetchSampleResourceAll(
				Collections.singleton(FilterBuilder.create().onAccession("SAM[^E].*").build()))) {
			existingAccessions.add(sample.getContent().getAccession());
		}	
		
		long endTime = System.nanoTime();
		double intervalSec = ((double)(endTime-startTime))/1000000000.0;
		log.debug("Took "+intervalSec+"s to get "+existingAccessions.size()+" existing public ncbi accessions");	
		return existingAccessions;
	}
	
	private void makePrivate(Set<String> toRemoveAccessions) {
		//TODO make this multithreaded for performance
		for (String accession : toRemoveAccessions) {
			// this must get the ORIGINAL sample without curation
			Optional<Resource<Sample>> sampleOptional = bioSamplesClient.fetchSampleResource(accession, Optional.empty());
			
			if (sampleOptional.isPresent()) {
				Sample sample = sampleOptional.get().getContent();
				
				//set the release date to 1000 years in the future to make it private again
//				Sample newSample = Sample.build(sample.getName(),
//						sample.getAccession(),
//						sample.getDomain(),
//						ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant(),
//						sample.getUpdate(),
//						sample.getAttributes(),
//						sample.getRelationships(),
//						sample.getExternalReferences(),
//						sample.getOrganizations(),
//						sample.getContacts(),
//						sample.getPublications());
                Sample newSample = Sample.Builder.fromSample(sample)
						.withRelease(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant())
						.build();
				
				//persist the now private sample
				log.debug("Making private "+sample.getAccession());
				bioSamplesClient.persistSampleResource(newSample);
			}
		}
	}

}
