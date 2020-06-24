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

    public Collection<String> validate(Relationship rel, String accession) {
        // TODO validate that relationships have this sample as the source
        Collection<String> errors =  validate(rel, new ArrayList<>());
        return validateSourceAccession(accession, rel, errors);
    }

    public Collection<String> validate(Relationship rel, Collection<String> errors) {
        if (rel.getSource() == null || rel.getSource().isEmpty()) {
//            errors.add("Source of a relationship must be non empty");//todo re-enable after samepletab deprecation
        } else if (!AccessionType.ANY.matches(rel.getSource())) {
            errors.add("Source of a relationship must be an accession but was \"" + rel.getSource() + "\"");
        }

        if (rel.getTarget() == null || rel.getTarget().isEmpty()) {
//            errors.add("Target of a relationship must be non empty");//todo re-enable after samepletab deprecation
        } else if(!AccessionType.ANY.matches(rel.getTarget())) {
            errors.add("Target of a relationship must be an accession but was \"" + rel.getTarget() + "\"");
        }

        return errors;
    }

    private Collection<String> validateSourceAccession(String accession, Relationship rel, Collection<String> errors) {
        if (accession != null && !accession.equals(rel.getSource())) {
//            errors.add("Source of the relationship must equal to the sample accession");// todo enable after fixing ENA import pipeline
        }

        return errors;
    }
}
