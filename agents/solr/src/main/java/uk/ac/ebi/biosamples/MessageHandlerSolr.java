package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.messages.threaded.MessageSampleStatus;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

@Service
public class MessageHandlerSolr {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	//@Autowired
	//private SolrSampleRepository solrSampleRepository;
	
	@Autowired
	private MessageBuffer messageBuffer;

	@RabbitListener(queues = Messaging.queueToBeIndexedSolr)
	public void handle(Sample sample) {
		
		log.info("Handling "+sample.getAccession());
		
		SolrSample solrSample = SolrSample.build(sample.getName(), sample.getAccession(), 
				sample.getRelease(), sample.getUpdate(), 
				sample.getAttributes(), sample.getRelationships());	
		
		//allow solr to wait up to 1 seconds before saving
		//solrSampleRepository.saveWithin(solrSample, 1000);
		
		
		MessageSampleStatus messageSampleStatus;
		try {
			messageSampleStatus = messageBuffer.recieve(solrSample);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		while (!messageSampleStatus.storedInSolr.get()) {			
			//wait a little bit
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		
	}
}
