package uk.ac.ebi.biosamples.mongo.util;

public class SampleNameAccessionPair {
    private String sampleName;
    private String sampleAccession;

    public SampleNameAccessionPair(final String sampleName, final String sampleAccession) {
        this.sampleName = sampleName;
        this.sampleAccession = sampleAccession;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public void setSampleAccession(String sampleAccession) {
        this.sampleAccession = sampleAccession;
    }

    public String getSampleName() {
        return sampleName;
    }

    public String getSampleAccession() {
        return sampleAccession;
    }
}
