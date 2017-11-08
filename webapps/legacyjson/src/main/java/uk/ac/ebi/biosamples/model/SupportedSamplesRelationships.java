package uk.ac.ebi.biosamples.model;

public enum SupportedSamplesRelationships {
    DERIVE_FROM("derivedFrom"),
    DERIVE_TO("derivedTo"),
    CHILD_OF("childOf"),
    PARENT_OF("parentOf"),
    RECURATED_FROM("recuratedFrom"),
    RECURATED_TO("recuratedTo"),
    SAME_AS("same_as"),
    GROUPS("groups"),
    EXTERNAL_LINKS("externalLinks");


    private String relationshipName;

    SupportedSamplesRelationships(String name) {
        this.relationshipName = name;
    }

    public String getRelationshipName() {
        return this.relationshipName;
    }

    public static SupportedSamplesRelationships getFromName(String name) {
        for (SupportedSamplesRelationships rel: values()) {
            if (rel.getRelationshipName().equals(name))
                return rel;
        }

        return null;
    }
}
