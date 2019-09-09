package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.StaticViewWrapper;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
public class MessagingService {
	private Logger log = LoggerFactory.getLogger(getClass());

	private final SampleReadService sampleReadService;
	private final AmqpTemplate amqpTemplate;
	private final MongoSampleRepository mongoSampleRepository;
	private final SampleToMongoSampleConverter sampleToMongoSampleConverter;
	
	public MessagingService(SampleReadService sampleReadService,
							AmqpTemplate amqpTemplate,
							MongoSampleRepository mongoSampleRepository,
							SampleToMongoSampleConverter sampleToMongoSampleConverter) {
		this.sampleReadService = sampleReadService;
		this.amqpTemplate = amqpTemplate;
		this.mongoSampleRepository = mongoSampleRepository;
		this.sampleToMongoSampleConverter = sampleToMongoSampleConverter;
	}
	
	public void fetchThenSendMessage(String accession) {
		fetchThenSendMessage(accession, Collections.emptyList());
	}

	public void fetchThenSendMessage(String accession, List<String> existingRelationshipTargets) {
		if (accession == null) throw new IllegalArgumentException("accession cannot be null");
		if (accession.trim().length() == 0) throw new IllegalArgumentException("accession cannot be empty");
		
		Optional<Sample> sample = sampleReadService.fetch(accession, Optional.empty());
		if (sample.isPresent()) {
			//save sample with curations and relationships in static view collection
			mongoSampleRepository.insertSampleToCollection(
					sampleToMongoSampleConverter.convert(sample.get()), StaticViewWrapper.StaticView.SAMPLES_CURATED);

			
			//for each sample we have a relationship to, update it to index this sample as an inverse relationship	
			//TODO do this async
			List<Sample> related = updateInverseRelationships(sample.get(), existingRelationshipTargets);

			//send the original sample with the extras as related samples
			amqpTemplate.convertAndSend(Messaging.exchangeForIndexingSolr, "", 
					MessageContent.build(sample.get(), null, related, false));
		}
		
	}

	private List<Sample> updateInverseRelationships(Sample sample, List<String> existingRelationshipTargets) {
		List<Future<Optional<Sample>>> futures = new ArrayList<>();

		//remove deleted relationships
		for (String accession : existingRelationshipTargets) {
			futures.add(sampleReadService.fetchAsync(accession, Optional.empty()));
		}

		for (Relationship relationship : sample.getRelationships()) {
			if (relationship.getSource() != null
					&& relationship.getSource().equals(sample.getAccession())
					&& !existingRelationshipTargets.contains(sample.getAccession())) {
				futures.add(sampleReadService.fetchAsync(relationship.getTarget(), Optional.empty()));
			}
		}

		List<Sample> related = new ArrayList<>();
		for (Future<Optional<Sample>> future : futures) {
			try {
				Optional<Sample> optionalSample = future.get();
				if (optionalSample.isPresent()) {
					related.add(optionalSample.get());
					//todo if we  add inverse relationships we also have to think about deleting them
					mongoSampleRepository.insertSampleToCollection(
							sampleToMongoSampleConverter.convert(optionalSample.get()), StaticViewWrapper.StaticView.SAMPLES_CURATED);
				}
			} catch (InterruptedException e) {
				log.warn("Interrupted fetching future relationships", e);
			} catch (ExecutionException e) {
				log.error("Problem fetching future relationships", e);
			}
		}
		return related;
	}

	@Deprecated
	public void sendMessages(CurationLink curationLink) {
		fetchThenSendMessage(curationLink.getSample());
	}
	

	@Deprecated
	public void sendMessages(Sample sample) {
		fetchThenSendMessage(sample.getAccession());
	}
	
	public List<Sample> getDerivedFromSamples(Sample sample, List<Sample> related) {
		
		for (Relationship relationship : sample.getRelationships()) {
			if (relationship.getSource().equals(sample.getAccession())) {
				if (relationship.getType().toLowerCase().equals("derived from")) {
					Optional<Sample> target = sampleReadService.fetch(relationship.getTarget(), Optional.empty());
					if (target.isPresent()) {
						if (!related.contains(target.get())) {
							related.add(target.get());
							getDerivedFromSamples(target.get(), related);
						}
					}
				}
			}
		}
		return related;
	}
	
}
