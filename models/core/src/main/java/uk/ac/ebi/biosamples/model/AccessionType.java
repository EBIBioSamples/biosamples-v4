package uk.ac.ebi.biosamples.model;

public enum AccessionType {
    ANY("SAM[END][AG]?[0-9]+"),
    ANY_GROUP("SAMEG[0-9]+"),
    ANY_SAMPLE("SAM[END][A]?[0-9]+"),
    NCBI_SAMPLE("SAMN[0-9]+"),
    EBI_SAMPLE("SAME[AG]?[0-9]+"),
    DDBJ_SAMPLE("SAMD[0-9]+");

    private final String accessionRegex;

    AccessionType(String regex) {
        this.accessionRegex = regex;
    }

    public String getAccessionRegex() {
        return this.accessionRegex;
    }

    public boolean matches(String accession) {
        return accession.matches(this.accessionRegex);
    }

}
