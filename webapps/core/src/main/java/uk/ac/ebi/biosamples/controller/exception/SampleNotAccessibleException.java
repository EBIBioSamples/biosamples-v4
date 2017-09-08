package uk.ac.ebi.biosamples.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Sample not accessible") // 403
public class SampleNotAccessibleException extends RuntimeException {

	private static final long serialVersionUID = -6250819256457895445L;
}