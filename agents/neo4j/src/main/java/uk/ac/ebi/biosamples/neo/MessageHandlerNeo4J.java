package uk.ac.ebi.biosamples.neo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.messages.threaded.MessageSampleStatus;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.neo.service.SampleToNeoSampleConverter;

@Service
public class MessageHandlerNeo4J {
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	@Qualifier("NeoSampleMessageBuffer")
	private MessageBuffer<NeoSample> messageBuffer;
	
	@Autowired
	private SampleToNeoSampleConverter sampleToNeoSampleConverter;

	@RabbitListener(queues = Messaging.queueToBeIndexedNeo4J)
	public void handle(Sample sample) {
		log.trace("Handling "+sample.getAccession());
		
		NeoSample neoSample = sampleToNeoSampleConverter.convert(sample);
				
		MessageSampleStatus<NeoSample> messageSampleStatus;
		try {
			messageSampleStatus = messageBuffer.recieve(neoSample);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		while (!messageSampleStatus.storedInRepository.get()) {			
			//wait a little bit
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		log.trace("Handed "+sample.getAccession());
	}
}
