package uk.ac.ebi.biosamples;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class MessageHandlerCuration {

	@RabbitListener(queues = Messaging.queueToBeCurated)
	public void handle(Sample sample) {
		//TODO write me
	}

}
