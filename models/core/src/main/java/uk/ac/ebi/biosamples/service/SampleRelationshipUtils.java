package uk.ac.ebi.biosamples.service;

import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SampleRelationshipUtils {

    public static SortedSet<Relationship> getOutgoingRelationships(Sample sample) {
        return sample.getRelationships().stream().filter(rel -> rel.getSource().equals(sample.getAccession())).collect(Collectors.toCollection(TreeSet::new));
    }

    public static SortedSet<Relationship> getIncomingRelationships(Sample sample) {
        return sample.getRelationships().stream()
                .filter(rel -> rel.getTarget().equals(sample.getAccession()))
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
