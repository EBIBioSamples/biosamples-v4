package uk.ac.ebi.biosamples.exceptions;

public class AssertingSampleTabOwnershipException extends SampleTabException {

    private static final long serialVersionUID = -1504945560846665587L;
    public final String submissionIdentifier;

    public AssertingSampleTabOwnershipException(String submissionIdentifier) {
        super("Submission identifier "+submissionIdentifier+" has not been previously submitted");
        this.submissionIdentifier = submissionIdentifier;
    }
}
