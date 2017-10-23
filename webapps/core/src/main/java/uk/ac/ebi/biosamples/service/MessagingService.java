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

	private final SampleService sampleService;
	private final AmqpTemplate amqpTemplate;
	
	public MessagingService(SampleService sampleService, AmqpTemplate amqpTemplate) {
		this.sampleService = sampleService;
		this.amqpTemplate = amqpTemplate;
	}
	
	public void sendMessages(CurationLink curationLink) {
		Optional<Sample> target = sampleService.fetch(curationLink.getSample());
		if (target.isPresent()) {
			sendMessages(target.get());
		}
	}
	

	public void sendMessages(Sample sample) {
		
		//for each sample we have a relationship to, update it to index this sample as an inverse relationship	
		for (Relationship relationship : sample.getRelationships()) {
			if (relationship.getSource().equals(sample.getAccession())) {
				Optional<Sample> target = sampleService.fetch(relationship.getTarget());
				if (target.isPresent()) {
					amqpTemplate.convertAndSend(Messaging.exchangeForIndexingSolr, "", 
							MessageContent.build(target.get(), null, Collections.emptyList(), false));
				}
			}
		}
		
		//repeatedly walk up the derived from tree and store as related
		//if this was a derived from relationship, this is a related sample to the original one
		
		List<Sample> related = new ArrayList<>();
		//related.addAll(getDerivedFromSamples(sample, related));
		//this is too intensive to do here, will need to move expansion to separate agent see BSD-904
		
		
		//send the original sample with the extras as related samples
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexingSolr, "", 
				MessageContent.build(sample, null, related, false));
		
	}
	
	public List<Sample> getDerivedFromSamples(Sample sample, List<Sample> related) {
		
		for (Relationship relationship : sample.getRelationships()) {
			if (relationship.getSource().equals(sample.getAccession())) {
				if (relationship.getType().toLowerCase().equals("derived from")) {
					Optional<Sample> target = sampleService.fetch(relationship.getTarget());
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
