package uk.ac.ebi.biosamples.model;

public enum SupportedGroupsRelationships {
    SAMPLES("samples"),
    EXTERNAL_LINKS("externallinks");


    private String relationshipName;

    SupportedGroupsRelationships(String name) {
        this.relationshipName = name;
    }

    public String getRelationshipName() {
        return this.relationshipName;
    }
}
