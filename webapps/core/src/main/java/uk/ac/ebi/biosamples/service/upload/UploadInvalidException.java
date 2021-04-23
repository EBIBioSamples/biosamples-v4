package uk.ac.ebi.biosamples.service.upload;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
        value = HttpStatus.BAD_REQUEST)
public class UploadInvalidException extends RuntimeException {
    public UploadInvalidException(final String collect) {
        super(collect);
    }
}
