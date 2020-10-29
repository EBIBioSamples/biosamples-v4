package uk.ac.ebi.biosamples.model.ga4gh.phenopacket;

public class PhenopacketAttribute {
    private String type;
    private String value;
    private String ontologyId;
    private String ontologyLabel;
    private boolean negate;

    private PhenopacketAttribute() {
        //hide constructor
    }

    public static PhenopacketAttribute build(String type, String value, String ontologyId, String ontologyLabel, boolean negate) {
        PhenopacketAttribute phenopacketAttribute = new PhenopacketAttribute();
        phenopacketAttribute.type = type;
        phenopacketAttribute.value = value;
        phenopacketAttribute.ontologyId = ontologyId;
        phenopacketAttribute.ontologyLabel = ontologyLabel;
        phenopacketAttribute.negate = negate;

        return phenopacketAttribute;
    }

    public static PhenopacketAttribute build(String type, String value, String ontologyId, String ontologyLabel) {
        return build(type, value, ontologyId, ontologyLabel, false);
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getOntologyId() {
        return ontologyId;
    }

    public String getOntologyLabel() {
        return ontologyLabel;
    }

    public boolean isNegate() {
        return negate;
    }
}
