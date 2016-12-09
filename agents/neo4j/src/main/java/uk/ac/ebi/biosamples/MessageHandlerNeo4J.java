package uk.ac.ebi.biosamples;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.models.NeoRelationship;
import uk.ac.ebi.biosamples.models.NeoSample;
import uk.ac.ebi.biosamples.models.Relationship;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.repos.NeoRelationshipRepository;
import uk.ac.ebi.biosamples.repos.NeoSampleRepository;

@Service
public class MessageHandlerNeo4J {

	@Autowired
	private NeoSampleRepository neoSampleRepository;

	@Autowired
	private NeoRelationshipRepository neoRelRepository;

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
		NeoSample neoSample = neoSampleRepository.findByAccession(sample.getAccession());
		if (neoSample == null) {
			// make a new one
			neoSample = new NeoSample(sample.getAccession());
			neoSample = neoSampleRepository.save(neoSample);
		}

		for (Relationship rel : sample.getRelationships()){

			// convert the target accession into a target object
			NeoSample targetSample = neoSampleRepository.findByAccession(rel.getTarget());
			
			// if it doesn't exist, create it
			if (targetSample == null) {
				targetSample = new NeoSample(rel.getTarget());
				targetSample = neoSampleRepository.save(targetSample);
			}
			NeoRelationship neorel = NeoRelationship.create(neoSample, targetSample, rel.getType());
			neorel = neoRelRepository.save(neorel);
			neoSample.getRelationships().add(neorel);
		}

		neoSample = neoSampleRepository.save(neoSample);
		
	}
}
