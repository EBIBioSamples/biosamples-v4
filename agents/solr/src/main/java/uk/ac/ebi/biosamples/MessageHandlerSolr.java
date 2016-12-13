package uk.ac.ebi.biosamples;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.models.SolrSample;
import uk.ac.ebi.biosamples.repo.SolrSampleRepository;

@Service
public class MessageHandlerSolr {

	@Autowired
	private SolrSampleRepository solrSampleRepository;

	@RabbitListener(queues = Messaging.queueToBeIndexedSolr)
	public void handle(Sample sample) {
		SolrSample solrSample = SolrSample.build(sample.getName(), sample.getAccession(), sample.getRelease(), sample.getUpdate(), null, null);		
		solrSampleRepository.save(solrSample);		
	}
}
