package uk.ac.ebi.biosamples.mongo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;

import uk.ac.ebi.biosamples.models.Relationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.service.InverseRelationshipService;

public class SampleResourceProcessor implements ResourceProcessor<Resource<MongoSample>> {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	public InverseRelationshipService inverseRelationshipService;
	
	@Override
	public Resource<MongoSample> process(Resource<MongoSample> resource) {
		log.debug("ResourceProcessor handling "+resource.getContent().accession);
		
		inverseRelationshipService.addInverseRelationships(resource.getContent());
		
		return resource;
	}

}
