package uk.ac.ebi.biosamples.exception;

//@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such Sample") // 404
public class SampleNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1376682660925892995L;
}