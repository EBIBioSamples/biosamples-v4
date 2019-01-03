package uk.ac.ebi.biosamples.exception;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.ac.ebi.biosamples.BioSamplesProperties;

@ControllerAdvice
public class GeneralExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    private final BioSamplesProperties bioSamplesProperties;

    public GeneralExceptionControllerAdvice(BioSamplesProperties bioSamplesProperties) {
        this.bioSamplesProperties = bioSamplesProperties;
    }

    @ExceptionHandler(value = {Exception.class})
    protected ModelAndView handleConflict(Exception exception, WebRequest request) throws Exception {
        // If the exception is annotated with @ResponseStatus rethrow it and let the framework handle it
        if (AnnotationUtils.findAnnotation(exception.getClass(), ResponseStatus.class) != null) {
            throw exception;
        }

        // Otherwise setup and send the user to a default error-view.
        ModelAndView mav = new ModelAndView();
        mav.addObject("exception", exception);
        mav.addObject("status", "Failed to process the request");
        mav.addObject("error", exception.getMessage());
        //todo setup a default error view
        mav.setViewName("error/4xx");
        mav.addObject("sampletabUrl", bioSamplesProperties.getBiosamplesWebappSampletabUri());
        return mav;
    }
}