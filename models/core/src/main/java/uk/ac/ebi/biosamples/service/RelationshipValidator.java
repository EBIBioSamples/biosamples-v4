package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.AccessionType;
import uk.ac.ebi.biosamples.model.Relationship;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class RelationshipValidator {

    public Collection<String> validate(Relationship rel) {
        return validate(rel, new ArrayList<>());
    }

    public Collection<String> validate(Relationship rel, Collection<String> errors) {


        if (rel.getSource() != null && !rel.getSource().isEmpty()) {
            if (!AccessionType.ANY.matches(rel.getSource())) {
                errors.add("Source of a relationship must be an accession but was \"" + rel.getSource() + "\"");
            }
        }

        if (rel.getTarget() != null && !rel.getTarget().isEmpty()) {
            if (!AccessionType.ANY.matches(rel.getTarget())) {
                errors.add("Target of a relationship must be an accession but was \"" + rel.getTarget() + "\"");
            }
        }

        return errors;

    }
}
