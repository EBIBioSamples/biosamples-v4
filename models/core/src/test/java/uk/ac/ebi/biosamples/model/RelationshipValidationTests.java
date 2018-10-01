package uk.ac.ebi.biosamples.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.service.RelationshipValidator;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class RelationshipValidationTests {


    private final RelationshipValidator relationshipValidator;

    public RelationshipValidationTests() {
        relationshipValidator = new RelationshipValidator();
    }

    @Test
    public void throws_exception_if_relationship_source_or_target_are_not_accessions() {
        Relationship rel = Relationship.build("Animal", "derivedFrom", "SAMEG1234123");
        Collection<String> errors = relationshipValidator.validate(rel);
        assertThat(errors).containsExactly("Source of a relationship must be an accession but was \""+rel.getSource()+"\"");
    }
}
