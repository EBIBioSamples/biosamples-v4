package uk.ac.ebi.biosamples.solr;

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
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.service.SampleToSolrSampleConverter;

@Service
public class MessageHandlerSolr {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	@Qualifier("SolrSampleMessageBuffer")
	private MessageBuffer<SolrSample> messageBuffer;
	
	@Autowired
	private SampleToSolrSampleConverter sampleToSolrSampleConverter;

	@RabbitListener(queues = Messaging.queueToBeIndexedSolr)
	public void handle(Sample sample) {
		
		log.trace("Handling "+sample.getAccession());
		
		SolrSample solrSample = sampleToSolrSampleConverter.convert(sample);
				
		MessageSampleStatus<SolrSample> messageSampleStatus;
		try {
			messageSampleStatus = messageBuffer.recieve(solrSample);
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
		
	}
}
