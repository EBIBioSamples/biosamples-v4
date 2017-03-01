package uk.ac.ebi.biosamples;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoRelationshipRepository;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.neo.service.SampleToNeoSampleConverter;

@Service
public class MessageHandlerNeo4J {

	@Autowired
	private NeoSampleRepository neoSampleRepository;
	
	@Autowired
	private SampleToNeoSampleConverter sampleToNeoSampleConverter;

	@RabbitListener(queues = Messaging.queueToBeIndexedNeo4J)
	public void handle(Sample sample) {
		//see if this sample has any relationships at all
		if (sample.getRelationships() == null || sample.getRelationships().size() == 0) {
			return;
		} else {
			persist(sample);
		}
	}

	@Transactional
	private void persist(Sample sample) {
		
		NeoSample neoSample = sampleToNeoSampleConverter.convert(sample);
		neoSample = neoSampleRepository.save(neoSample);
	}
}
