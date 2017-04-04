package uk.ac.ebi.biosamples;

import org.neo4j.driver.v1.exceptions.TransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.neo.service.SampleToNeoSampleConverter;

@Service
public class MessageHandlerNeo4J {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private NeoSampleRepository neoSampleRepository;
	
	@Autowired
	private SampleToNeoSampleConverter sampleToNeoSampleConverter;

	@RabbitListener(queues = Messaging.queueToBeIndexedNeo4J)
	public void handle(Sample sample) {
		NeoSample neoSample = sampleToNeoSampleConverter.convert(sample);
		try {
			neoSample = neoSampleRepository.save(neoSample);
		} catch (TransientException e) {
			//recurse for now
			log.warn("Transient exception saving sample "+sample.getAccession());
			handle(sample);
		}
	}
}
