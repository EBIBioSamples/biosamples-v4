package uk.ac.ebi.biosamples;

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

public class SampleResourceProcessor implements ResourceProcessor<Resource<MongoSample>> {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	public NeoSampleRepository neoSampleRepository;
	
	@Override
	public Resource<MongoSample> process(Resource<MongoSample> resource) {
		log.debug("ResourceProcessor handling "+resource.getContent().accession);
		
		NeoSample neoSample = neoSampleRepository.findOneByAccession(resource.getContent().accession);
		
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
				
				resource.getContent().getRelationships().add(rel);
				
				log.trace("Adding relationship from "+source+" to "+target);
			}
		}
		
		return resource;
	}

}
