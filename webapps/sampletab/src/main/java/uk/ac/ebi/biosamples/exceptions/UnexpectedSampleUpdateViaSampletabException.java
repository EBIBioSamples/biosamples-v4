package uk.ac.ebi.biosamples.exceptions;

public class UnexpectedSampleUpdateViaSampletabException extends SampleTabException {

    String accession;

    public UnexpectedSampleUpdateViaSampletabException(String accession) {
        super("Provided sample with accession " + accession + " should not be updated with the provided SampleTab because not generated within the same submission process");
        this.accession = accession;
    }
}
