package uk.ac.ebi.biosamples.exceptions;

public class SampleTabWithUnacceptableAccessionException extends SampleTabException {

    public final String accession;

    public SampleTabWithUnacceptableAccessionException(String accession) {
        super("Pre-existing accession "+accession+" cannot be used to fill new sampletab file");
        this.accession = accession;
    }
}
