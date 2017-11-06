package uk.ac.ebi.biosamples;

import uk.ac.ebi.biosamples.model.Attribute;

public class TestAttribute {

    public final String type;
    public final String value;
    private String ontologyUri;
    private String unit;

    public TestAttribute(String type, String value) {
        this.type = type;
        this.value = value;
        this.ontologyUri = "";
        this.unit = "";
    }

    public TestAttribute withOntologyUri(String ontologyUri) {
        this.ontologyUri = ontologyUri;
        return this;
    }

    public TestAttribute withUnit(String unit) {
        this.unit = unit;
        return this;
    }

    public Attribute build() {
        return Attribute.build(this.type, this.value, this.ontologyUri, this.unit);
    }


}
