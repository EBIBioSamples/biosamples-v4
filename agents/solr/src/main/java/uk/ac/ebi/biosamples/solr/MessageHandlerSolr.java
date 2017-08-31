package uk.ac.ebi.biosamples.solr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.messages.threaded.MessageSampleStatus;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.service.SampleToSolrSampleConverter;

@Service
public class MessageHandlerSolr {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private SolrMessageBuffer messageBuffer;
	
	@Autowired
	private SampleToSolrSampleConverter sampleToSolrSampleConverter;
	
	@RabbitListener(queues = Messaging.queueToBeIndexedSolr)
	public void handle(MessageContent messageContent) throws Exception {
		
		if (messageContent.getSample() == null) {
			log.warn("Recieved message without sample");
			return;
		}
		
		Sample sample = messageContent.getSample();		
		SolrSample solrSample = sampleToSolrSampleConverter.convert(sample);
		
		//TODO expand this conversion - OLS, related external references, etc
				
		MessageSampleStatus<SolrSample> messageSampleStatus;
		try {
			messageSampleStatus = messageBuffer.recieve(solrSample.getAccession(), solrSample, messageContent.getCreationTime());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		while (!messageSampleStatus.storedInRepository.get()
				&& messageSampleStatus.hadProblem.get() == null) {			
			//wait a little bit
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		
		if (messageSampleStatus.hadProblem.get() != null) {
			throw messageSampleStatus.hadProblem.get();
		}

		log.info("Handed "+sample.getAccession());
		
	}
}
