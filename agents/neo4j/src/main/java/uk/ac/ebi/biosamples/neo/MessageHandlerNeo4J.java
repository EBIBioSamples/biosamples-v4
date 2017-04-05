package uk.ac.ebi.biosamples.neo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private NeoMessageBuffer messageBuffer;

	@Autowired
	private NeoSampleRepository repository;
	
	@Autowired
	private SampleToNeoSampleConverter sampleToNeoSampleConverter;

	@RabbitListener(queues = Messaging.queueToBeIndexedNeo4J)
	//@Transactional
	public void handle(Sample sample) {
		log.trace("Handling "+sample.getAccession());
		
		NeoSample neoSample = sampleToNeoSampleConverter.convert(sample);
		
		MessageSampleStatus<NeoSample> messageSampleStatus;
		try {
			messageSampleStatus = messageBuffer.recieve(neoSample);
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
		
		
		//repository.save(neoSample);
		
		log.trace("Handed "+sample.getAccession());
	}
}
