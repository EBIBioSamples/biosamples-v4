package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

@Service
public class MessageHandlerSolr {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private SolrSampleRepository solrSampleRepository;

	@RabbitListener(queues = Messaging.queueToBeIndexedSolr)
	@Transactional
	public void handle(Sample sample) {
		
		log.info("Handling "+sample.getAccession());
		
		SolrSample solrSample = SolrSample.build(sample.getName(), sample.getAccession(), 
				sample.getRelease(), sample.getUpdate(), 
				sample.getAttributes(), sample.getRelationships());		
		solrSampleRepository.save(solrSample);		
	}
}
