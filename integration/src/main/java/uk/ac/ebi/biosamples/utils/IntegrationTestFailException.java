package uk.ac.ebi.biosamples.utils;

import uk.ac.ebi.biosamples.Phase;

public class IntegrationTestFailException extends RuntimeException {

    public IntegrationTestFailException(String message) {
        super(message);
    }

    public IntegrationTestFailException(String message, Phase phase) {
        super("Phase-" + phase + ": " + message);
    }
}
