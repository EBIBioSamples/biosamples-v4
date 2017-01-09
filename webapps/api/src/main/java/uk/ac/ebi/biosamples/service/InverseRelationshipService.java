package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.models.Relationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;

/**
 * Service layer buisness logic for adding inverse relationships to samples. Queries the Neo4J repository.
 * 
 * @author faulcon
 *
 */
@Service
public class InverseRelationshipService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public NeoSampleRepository neoSampleRepository;
	
	public InverseRelationshipService(@Autowired NeoSampleRepository neoSampleRepository) {
		this.neoSampleRepository = neoSampleRepository;
	}
	
	public void addInverseRelationships(MongoSample sample) {

		NeoSample neoSample = neoSampleRepository.findOneByAccession(sample.accession);
		
		log.trace("neoSample = "+neoSample);
		if (neoSample != null) {
			log.trace("neoSample.getRelationships() = "+neoSample.getRelationships());
		}
		
		if (neoSample != null 
				&& neoSample.getRelationships() != null 
				&& neoSample.getRelationships().size() > 0) {
			
			for(NeoRelationship neoRelationship : neoSample.getRelationships()) {
				String target = neoRelationship.getTarget().getAccession();
				String source = neoRelationship.getOwner().getAccession();
				String relType = neoRelationship.getSpecificType();
				Relationship rel = Relationship.build(relType, target, source);
				
				sample.addRelationship(rel);
				
				log.trace("Adding relationship from "+source+" to "+target);
			}
		}
	}
}
