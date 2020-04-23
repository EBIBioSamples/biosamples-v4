package uk.ac.ebi.biosamples.model;

public enum RelationshipType {
    DERIVED_FROM,
    SAME_AS,
    CHILD_OF,
    HAS_MEMBER,
    OTHER;

    public static RelationshipType getType(String relationshipTypeString) {
        RelationshipType type;
        try {
            type = RelationshipType.valueOf(relationshipTypeString.replaceAll(" ", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            type = RelationshipType.OTHER;
        }

        return type;
    }
}
