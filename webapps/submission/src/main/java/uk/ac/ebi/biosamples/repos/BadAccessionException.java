package uk.ac.ebi.biosamples.repos;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Missing or invalid accession")
public class BadAccessionException extends Exception {

	private static final long serialVersionUID = -4850732315502756434L;
}