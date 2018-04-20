package uk.ac.ebi.biosamples.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationLinkRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

/**
 * Service layer business logic for centralising repository access and
 * conversions between different controller. Use this instead of linking to
 * repositories directly.
 * 
 * @author faulcon
 *
 */
@Service
public class SamplePageService {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private MongoSampleRepository mongoSampleRepository;
	@Autowired
	private MongoCurationLinkRepository mongoCurationLinkRepository;
		
	//TODO use a ConversionService to manage all these
	@Autowired
	private MongoSampleToSampleConverter mongoSampleToSampleConverter;

	@Autowired
	private SampleReadService sampleService;
	
	@Autowired
	private SolrSampleService solrSampleService;
	
	
	
	public Page<Sample> getSamplesByText(String text, Collection<Filter> filters, Collection<String> domains, Pageable pageable) {
		long startTime = System.nanoTime();
		Page<SolrSample> pageSolrSample = solrSampleService.fetchSolrSampleByText(text, filters, domains, pageable);
		long endTime = System.nanoTime();
		log.trace("Got solr page in "+((endTime-startTime)/1000000)+"ms");
		//for each result fetchUsing the stored version and add e.g. inverse relationships
		//stream process each into a sample

		startTime = System.nanoTime();
		Page<Future<Optional<Sample>>> pageFutureSample = pageSolrSample.map(ss -> sampleService.fetchAsync(ss.getAccession(), Optional.empty()));
		Page<Sample> pageSample = pageFutureSample.map(ss->{
			try {
				if (ss.get().isPresent()) {
					return ss.get().get();
				} else {
					return null;
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		});
		endTime = System.nanoTime();
		log.trace("Got mongo page content in "+((endTime-startTime)/1000000)+"ms");
		return pageSample;
	}	

	public Page<Sample> getSamplesOfExternalReference(String urlHash, Pageable pageable) {
		Page<MongoSample> pageMongoSample = mongoSampleRepository.findByExternalReferences_Hash(urlHash, pageable);		
		//convert them into a state to return
		Page<Sample> pageSample = pageMongoSample.map(mongoSampleToSampleConverter);
		return pageSample;
	}

	public Page<Sample> getSamplesOfCuration(String hash, Pageable pageable) {
		Page<MongoCurationLink> accession = mongoCurationLinkRepository.findByCurationHash(hash, pageable);
		//stream process each into a sample
		Page<Sample> pageSample = accession.map(mcl -> sampleService.fetch(mcl.getSample(), Optional.empty()).get());			
		return pageSample;
	}
	
	public CursorArrayList<Sample> getSamplesByText(String text, Collection<Filter> filters, 
			Collection<String> domains, String cursorMark, int size) {
		
		if (cursorMark == null || cursorMark.trim().length() == 0) {
			cursorMark = "*";
		}
		if (size > 1000) {
			size = 1000;
		}
		if (size < 1) {
			size = 1;
		}
		
		long startTime = System.nanoTime();
		CursorArrayList<SolrSample> cursorSolrSample = solrSampleService.fetchSolrSampleByText(text, filters, domains, cursorMark, size);
		long endTime = System.nanoTime();
		log.trace("Got solr cursor in "+((endTime-startTime)/1000000)+"ms");

		startTime = System.nanoTime();
		List<Future<Optional<Sample>>> listFutureSample = cursorSolrSample.stream()
				.map(s -> sampleService.fetchAsync(s.getAccession(), Optional.empty()))
				.collect(Collectors.toList());
		List<Sample> listSample = listFutureSample.stream().map(ss->{
			try {
				return ss.get().get();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
		endTime = System.nanoTime();
		log.trace("Got mongo page content in "+((endTime-startTime)/1000000)+"ms");
		
		CursorArrayList<Sample> cursorSample = new CursorArrayList<>(listSample, cursorSolrSample.getNextCursorMark());
		return cursorSample;
	}
}
