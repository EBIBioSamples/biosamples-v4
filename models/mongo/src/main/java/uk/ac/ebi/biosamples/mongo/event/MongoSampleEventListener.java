package uk.ac.ebi.biosamples.mongo.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import uk.ac.ebi.biosamples.mongo.model.MongoExternalReference;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoExternalReferenceRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoRelationshipRepository;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class MongoSampleEventListener extends AbstractMongoEventListener<MongoSample> {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final MongoExternalReferenceRepository mongoExternalReferenceRepository;	
	private final MongoRelationshipRepository mongoRelationshipRepository;
	
	
	public MongoSampleEventListener(MongoExternalReferenceRepository mongoExternalReferenceRepository, MongoRelationshipRepository mongoRelationshipRepository) {
		super();
		this.mongoExternalReferenceRepository = mongoExternalReferenceRepository;
		this.mongoRelationshipRepository = mongoRelationshipRepository;
	}
	


	/**
	 * Before converting, make sure nested external references and relationships already exist in the database
	 * 
	 */
	@Override
	public void onBeforeConvert(BeforeConvertEvent<MongoSample> event) {

		log.trace("processing onBeforeConvert for "+event.getSource());
		
		//put any external references in the external reference collection		
		SortedSet<MongoExternalReference> loadedExternalReferences = new TreeSet<>();		
		for (MongoExternalReference externalReference : event.getSource().getExternalReferences()) {
			//if it already exists, no need to save
			if (mongoExternalReferenceRepository.findOne(externalReference.getHash()) == null) {
				externalReference = mongoExternalReferenceRepository.save(externalReference);
			}
			loadedExternalReferences.add(externalReference);

		}		
		//update the source object
		event.getSource().getExternalReferences().clear();
		event.getSource().getExternalReferences().addAll(loadedExternalReferences);

		

		//find and delete any relationships that are no longer on the sample
		for (MongoRelationship relationship : mongoRelationshipRepository.findAllBySource(event.getSource().getAccession())) {
			if (!event.getSource().getRelationships().contains(relationship)) {
				log.debug("Deleting "+relationship);
				mongoRelationshipRepository.delete(relationship);
			}
		}
		//put any external references in the external reference collection		
		SortedSet<MongoRelationship> loadedRelationships = new TreeSet<>();		
		for (MongoRelationship relationship : event.getSource().getRelationships()) {
			//if it already exists, no need to save
			if (mongoRelationshipRepository.findOne(relationship.getHash()) == null) {
				relationship = mongoRelationshipRepository.save(relationship);
			}
			loadedRelationships.add(relationship);
		}		
		//update the source object
		event.getSource().getRelationships().clear();
		event.getSource().getRelationships().addAll(loadedRelationships);
	}
	
	
	/**
	 * During retrieval, add inverse relationships
	 * 
	 */
	@Override
	public void onAfterConvert(AfterConvertEvent<MongoSample> event) {
		log.trace("processing onAfterConvert for "+event.getSource());
		long startTime = System.nanoTime();
		List<MongoRelationship> relationships = mongoRelationshipRepository.findAllByTarget(event.getSource().getAccession());
		log.trace("Found "+relationships.size()+" relationships in "+((System.nanoTime()-startTime)/1000000l)+"ms");
		event.getSource().getRelationships().addAll(relationships);
	}

	@Override
	public void onAfterSave(AfterSaveEvent<MongoSample> event) {
		log.trace("processing onAfterConvert for "+event.getSource());
		List<MongoRelationship> relationships = mongoRelationshipRepository.findAllByTarget(event.getSource().getAccession());
		log.trace("Found relationships "+relationships);
		event.getSource().getRelationships().addAll(relationships);
	}
}
