package uk.ac.ebi.biosamples;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class MessageHandlerCuration {
	
	@Autowired
	public BioSamplesClient BioSamplesClient;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@RabbitListener(queues = Messaging.queueToBeCurated)
	public void handle(Sample sample) {		
		
		boolean changed = false;

		//TODO write me
				
		//query un-mapped attributes against Zooma
		
		//validate existing ontology terms against OLS
		
		//turn attributes with biosample accessions into relationships
		
		//split number+unit attributes
		
		//lowercase attribute types		
		//lowercase relationship types
		
	}

}
