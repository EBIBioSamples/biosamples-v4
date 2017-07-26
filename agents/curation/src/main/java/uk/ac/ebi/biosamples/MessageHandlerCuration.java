package uk.ac.ebi.biosamples;

import java.util.Collections;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class MessageHandlerCuration {
	
	
	private final BioSamplesClient bioSamplesClient;
	
	public MessageHandlerCuration(BioSamplesClient bioSamplesClient) {
		this.bioSamplesClient = bioSamplesClient;
	}
	
	/**
	 * 
	 * Takes a single Sample off the queue, and checks for the first curation it can find
	 * If it finds one, then it submits the curation via BioSamplesClient and ends. Once that
	 * curation has been loaded into the database, this sample will come back to this agent for 
	 * further curation.
	 * If no curation is found, no further action is taken.
	 * 
	 * 
	 * Once a sufficient library of curations has been built up, this should be replaced with
	 * application of curation from similar samples, not this crude brute-force approach
	 * 
	 * @param sample
	 */
	@RabbitListener(queues = Messaging.queueToBeCurated)
	public void handle(Sample sample) {		
		
	}

	
}
