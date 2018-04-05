package uk.ac.ebi.biosamples.controller;

import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public class GlobalControllerOptions {

    /**
     * Change the default Spring RequestParameter splitting behaviour on the filter parameter to split only on &
     */
    @InitBinder("filter")
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor( String[].class, new StringArrayPropertyEditor(null));
    }
}
