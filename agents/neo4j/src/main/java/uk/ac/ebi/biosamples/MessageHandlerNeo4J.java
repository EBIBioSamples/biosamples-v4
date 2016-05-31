package uk.ac.ebi.biosamples;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.models.SimpleSample;

@Component
public class MessageHandlerNeo4J {

	@RabbitListener(queues=Messaging.queueToBeIndexedNeo4J)
	public void handle(SimpleSample sample) {
		
	}
}
