package uk.ac.ebi.biosamples.legacy.json.domain;

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
