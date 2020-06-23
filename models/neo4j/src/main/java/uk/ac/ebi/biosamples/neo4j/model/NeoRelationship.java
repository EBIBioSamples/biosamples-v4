package uk.ac.ebi.biosamples.neo4j.model;

import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.RelationshipType;

public class NeoRelationship {
    private RelationshipType type;
    private String source;
    private String target;


    private NeoRelationship(RelationshipType type, String source, String target) {
        this.type = type;
        this.source = source;
        this.target = target;
    }

    public RelationshipType getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public static NeoRelationship build(Relationship relationship) {
        return new NeoRelationship(
                RelationshipType.getType(relationship.getType()), relationship.getSource(), relationship.getTarget());
    }
}
