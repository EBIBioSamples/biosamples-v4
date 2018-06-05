package uk.ac.ebi.biosamples.exceptions;

public class ConflictingSampleTabOwnershipException extends SampleTabException {

    private static final long serialVersionUID = -1504945560846665587L;
    public final String sampleAccession;
    public final String originalSubmission;
    public final String newSubmission;

    public ConflictingSampleTabOwnershipException(String sampleAccession, String originalSubmission, String newSubmission) {
        super("Accession "+sampleAccession+" was previously described in "+originalSubmission);
        this.sampleAccession = sampleAccession;
        this.originalSubmission = originalSubmission;
        this.newSubmission = newSubmission;
    }
}
