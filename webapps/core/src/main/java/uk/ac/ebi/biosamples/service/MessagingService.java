package uk.ac.ebi.biosamples.service;

import java.util.ArrayList;
import java.util.Collections;
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
	
	public void sendMessages(CurationLink curationLink) {
		Optional<Sample> target = sampleReadService.fetch(curationLink.getSample());
		if (target.isPresent()) {
			sendMessages(target.get());
		}
	}
	

	public void sendMessages(Sample sample) {
		
		//for each sample we have a relationship to, update it to index this sample as an inverse relationship	
		List<Sample> related = new ArrayList<>();
		for (Relationship relationship : sample.getRelationships()) {
			if (relationship.getSource() != null && relationship.getSource().equals(sample.getAccession())) {
				Optional<Sample> target = sampleReadService.fetch(relationship.getTarget());
				if (target.isPresent()) {
					related.add(target.get());
				}
			}
		}	
		
		//send the original sample with the extras as related samples
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexingSolr, "", 
				MessageContent.build(sample, null, related, false));
		
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
