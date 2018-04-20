package uk.ac.ebi.biosamples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.service.SampleReadService;
import uk.ac.ebi.biosamples.utils.ThreadUtils;


/**
 * This runner will get a list of accessions from mongo directly, query
 * the API to get the latest information, and then send that information
 * to Rabbit for the Solr Agent to reindex it into Solr.
 *
 * Mongo is queried instead of the API because the API is driven by Solr,
 * and if Solr is incorrect (which it will be because why else would you
 * run this) then it won't get the right information from the API.
 *
 * @author faulcon
 *
 */
@Component
public class ReindexRunner implements ApplicationRunner {
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private AmqpTemplate amqpTemplate;
	
	@Autowired
	private SampleReadService sampleReadService;
	
	@Autowired
	MongoOperations mongoOperations;

	@Override
	public void run(ApplicationArguments args) throws Exception {	
		Map<String, Future<Void>> futures =  new HashMap<>();
		
		ExecutorService executor = null;
		try {
			executor = Executors.newFixedThreadPool(128);

			try (CloseableIterator<MongoSample> it = mongoOperations.stream(new Query(), MongoSample.class)) {
				while (it.hasNext()) {
					MongoSample mongoSample = it.next();
					String accession = mongoSample.getAccession();
					log.info("handling sample "+accession);
					futures.put(accession, 
						executor.submit(
							new AccessionCallable(accession, sampleReadService, amqpTemplate)));
					ThreadUtils.checkFutures(futures, 1000);
				}
			}
			ThreadUtils.checkFutures(futures, 0);
		} finally {
			executor.shutdown();
			executor.awaitTermination(24, TimeUnit.HOURS);
		}
	}
	
	private static class AccessionCallable implements Callable<Void> {
		@SuppressWarnings("unused")
		private Logger log = LoggerFactory.getLogger(getClass());
		
		private final String accession;
		private final SampleReadService sampleReadService;
		private final AmqpTemplate amqpTemplate;
		private static final List<Sample> related = new ArrayList<>();
		
		public AccessionCallable(String accession, SampleReadService sampleReadService, AmqpTemplate amqpTemplate) {
			this.accession = accession;
			this.sampleReadService = sampleReadService;
			this.amqpTemplate = amqpTemplate;
		}

		@Override
		public Void call() throws Exception {
			amqpTemplate.convertAndSend(Messaging.exchangeForIndexingSolr, "", 
					MessageContent.build(sampleReadService.fetch(accession, Optional.empty()).get(), null, related, false));
			return null;
		}
	}

}
