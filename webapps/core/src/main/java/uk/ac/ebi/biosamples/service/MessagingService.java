package uk.ac.ebi.biosamples.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class MessagingService {

	private final SampleReadService sampleReadService;
	private final AmqpTemplate amqpTemplate;
	
	public MessagingService(SampleReadService sampleReadService, AmqpTemplate amqpTemplate) {
		this.sampleReadService = sampleReadService;
		this.amqpTemplate = amqpTemplate;
	}
	
	public void fetchThenSendMessage(String accession) {
		if (accession == null) throw new IllegalArgumentException("accession cannot be null");
		if (accession.trim().length() == 0) throw new IllegalArgumentException("accession cannot be empty");
		
		
		Optional<Sample> sample = sampleReadService.fetch(accession);
		if (sample.isPresent()) {
			
			//for each sample we have a relationship to, update it to index this sample as an inverse relationship	
			//TODO do this async
			List<Sample> related = new ArrayList<>();
			for (Relationship relationship : sample.get().getRelationships()) {
				if (relationship.getSource() != null 
						&& relationship.getSource().equals(accession)) {
					Optional<Sample> target = sampleReadService.fetch(relationship.getTarget());
					if (target.isPresent()) {
						related.add(target.get());
					}
				}
			}	
			
			//send the original sample with the extras as related samples
			amqpTemplate.convertAndSend(Messaging.exchangeForIndexingSolr, "", 
					MessageContent.build(sample.get(), null, related, false));
		}
		
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
					Optional<Sample> target = sampleReadService.fetch(relationship.getTarget());
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
