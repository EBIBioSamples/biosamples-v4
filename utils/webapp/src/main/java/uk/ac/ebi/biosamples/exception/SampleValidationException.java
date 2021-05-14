package uk.ac.ebi.biosamples.exception;

public class SampleValidationException extends RuntimeException {
    public SampleValidationException(String message) {
        super(message);
    }
}
