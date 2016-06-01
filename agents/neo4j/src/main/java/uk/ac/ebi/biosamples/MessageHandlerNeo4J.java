package uk.ac.ebi.biosamples;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.models.NeoRelationship;
import uk.ac.ebi.biosamples.models.NeoSample;
import uk.ac.ebi.biosamples.models.SimpleSample;
import uk.ac.ebi.biosamples.repos.NeoRelationshipRepository;
import uk.ac.ebi.biosamples.repos.NeoSampleRepository;

@Component
public class MessageHandlerNeo4J {

	@Autowired
	private NeoSampleRepository neoSampleRepository;

	@Autowired
	private NeoRelationshipRepository neoRelRepository;

	@RabbitListener(queues = Messaging.queueToBeIndexedNeo4J)
	@Transactional
	public void handle(SimpleSample sample) {
		NeoSample neoSample = neoSampleRepository.findByAccession(sample.getAccession());
		if (neoSample == null) {
			// make a new one
			neoSample = new NeoSample(sample.getAccession());
			neoSample = neoSampleRepository.save(neoSample);
		}

		for (String relType : sample.getRelationshipTypes()) {
			for (String targetAcc : sample.getRelationshipTargets(relType)) {
				// convert the target accession into a target object
				NeoSample targetSample = neoSampleRepository.findByAccession(targetAcc);
				// if it doesn't exist, create it
				if (targetSample == null) {
					targetSample = new NeoSample(targetAcc);
					targetSample = neoSampleRepository.save(targetSample);
				}
				NeoRelationship rel = NeoRelationship.create(neoSample, targetSample, relType);
				rel = neoRelRepository.save(rel);
				neoSample.getRelationships().add(rel);
			}
		}

		neoSample = neoSampleRepository.save(neoSample);
	}
}
