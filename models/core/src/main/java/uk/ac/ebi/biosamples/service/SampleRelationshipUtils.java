package uk.ac.ebi.biosamples.service;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleRelationshipUtils {
	
	/**
	 * Given a sample, get the collection of relationships where this sample is the source.
	 * 
	 * Sample will be the source if it matches the accession, or if it is blank if
	 * the sample has no accession. 
	 */
    public static SortedSet<Relationship> getOutgoingRelationships(Sample sample) {    	
    	SortedSet<Relationship> relationships = new TreeSet<>();
    	for (Relationship relationship : sample.getRelationships()) {
    		if (!sample.hasAccession() && 
    				(relationship.getSource() == null 
    					|| relationship.getSource().trim().length() == 0)) {
    			relationships.add(relationship);
    		} else if (relationship.getSource() != null 
    				&& relationship.getSource().equals(sample.getAccession())) {
    			relationships.add(relationship);
    		}
    	}
    	
        return relationships;
    }

	/**
	 * Given a sample, get the collection of relationships where this sample is the target.
	 * 
	 */
    public static SortedSet<Relationship> getIncomingRelationships(Sample sample) {
        return sample.getRelationships().stream()
                .filter(rel -> rel.getTarget().equals(sample.getAccession()))
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
