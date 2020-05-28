package uk.ac.ebi.biosamples.model;

public class AssertionEvidences {
    private String identifier;
    private String shortForm;
    private String label;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getShortForm() {
        return shortForm;
    }

    public void setShortForm(String shortForm) {
        this.shortForm = shortForm;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "AssertionEvidences [identifier = " + identifier + ", shortForm = " + shortForm + ", label = " + label + "]";
    }
}
