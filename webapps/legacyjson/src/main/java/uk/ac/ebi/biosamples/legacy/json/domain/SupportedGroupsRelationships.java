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

    public static SupportedGroupsRelationships getFromName(String name) {
        for (SupportedGroupsRelationships rel: values()) {
            if (rel.getRelationshipName().equals(name))
                return rel;
        }

        return null;
    }
}
