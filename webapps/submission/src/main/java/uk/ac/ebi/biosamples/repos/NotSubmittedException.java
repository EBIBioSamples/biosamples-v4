package uk.ac.ebi.biosamples.repos;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Updates must have an accession")
public class NotSubmittedException extends Exception {
}