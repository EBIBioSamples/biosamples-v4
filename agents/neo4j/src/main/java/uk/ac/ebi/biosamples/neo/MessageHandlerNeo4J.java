package uk.ac.ebi.biosamples.neo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.messages.threaded.MessageSampleStatus;

@Service
public class MessageHandlerNeo4J {
	private Logger log = LoggerFactory.getLogger(getClass());


	@Autowired
	private AmqpTemplate amqpTemplate;
	
	@Autowired
	private uk.ac.ebi.biosamples.neo.NeoMessageBuffer messageBuffer;

	@RabbitListener(queues = Messaging.queueToBeIndexedNeo4J)
	public void handle(MessageContent messageContent) {
				
		MessageSampleStatus<MessageContent> messageSampleStatus;
		try {
			messageSampleStatus = messageBuffer.recieve(messageContent);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		while (!messageSampleStatus.storedInRepository.get()
				&& !messageSampleStatus.hadProblem.isMarked()) {			
			//wait a little bit
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		
		if (messageSampleStatus.hadProblem.isMarked()) {
			throw messageSampleStatus.hadProblem.getReference();
		}

		// send a message for further processing if necessary
		//TODO bundle in additional information of relevant samples
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexingSolr, "", messageContent);
	}
}
