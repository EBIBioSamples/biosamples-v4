package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LegacyRelations {

    private String accession;


    @JsonCreator
    public LegacyRelations(@JsonProperty("accession") String accession) {
        this.accession = accession;
    }

    @JsonProperty
    public String accession() {
        return this.accession;
    }
}
