package uk.ac.ebi.biosamples.service;

import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;

/**
 * Converter for turning Sample objects in to Sample objects with inverse relationships from Neo4J added.
 * 
 * @author faulcon
 *
 */
@Service
public class InverseRelationshipService  {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public NeoSampleRepository neoSampleRepository;
	
	public InverseRelationshipService(@Autowired NeoSampleRepository neoSampleRepository) {
		this.neoSampleRepository = neoSampleRepository;
	}
	
	public Sample insertInverses(Sample sample) {
		NeoSample neoSample = neoSampleRepository.findOneByAccession(sample.getAccession());
		
		log.trace("neoSample = "+neoSample);
		if (neoSample != null) {
			log.trace("neoSample.getRelationships() = "+neoSample.getRelationships());
		}
		
		if (neoSample == null) {
			return sample;
		} else if (neoSample.getRelationships() == null 
				|| neoSample.getRelationships().size() == 0) {
			return sample;
		} else {			
			SortedSet<Relationship> relationships = new TreeSet<>();
			if (sample.getRelationships() != null && sample.getRelationships().size() > 0) {
				relationships.addAll(sample.getRelationships());
			}
			
			for(NeoRelationship neoRelationship : neoSample.getRelationships()) {
				String target = neoRelationship.getTarget().getAccession();
				String source = neoRelationship.getOwner().getAccession();
				String relType = neoRelationship.getSpecificType();
				Relationship rel = Relationship.build(relType, target, source);
				relationships.add(rel);
				log.trace("Adding relationship from "+source+" to "+target);
			}
			
			return Sample.build(sample.getName(), sample.getAccession(), sample.getRelease(), sample.getUpdate(), 
					sample.getAttributes(), relationships, sample.getExternalReferences());
		}
	}
}
