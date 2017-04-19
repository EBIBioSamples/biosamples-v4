package uk.ac.ebi.biosamples.model;

import org.springframework.hateoas.core.Relation;

@Relation(value="sample", collectionRelation = "samples")
public class SampleV3{

    private String accession;

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public static SampleV3 build(String accession) {
        SampleV3 newResource = new SampleV3();
        newResource.setAccession(accession);
        return newResource;
    }
}
