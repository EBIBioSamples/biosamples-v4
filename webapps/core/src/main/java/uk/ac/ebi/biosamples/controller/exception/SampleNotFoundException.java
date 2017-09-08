package uk.ac.ebi.biosamples.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such Sample") // 404
public class SampleNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1376682660925892995L;
}