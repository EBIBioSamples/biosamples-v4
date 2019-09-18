package uk.ac.ebi.biosamples.attributes;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.ac.ebi.biosamples.BioSamplesProperties;

import java.net.URI;

@ControllerAdvice
public class ModelAttributeControllerAdvice {

    private final BioSamplesProperties bioSamplesProperties;

    public ModelAttributeControllerAdvice(BioSamplesProperties bioSamplesProperties) {
        this.bioSamplesProperties = bioSamplesProperties;
    }

    @ModelAttribute("sampletabUrl")
    public URI addLink() {
        return bioSamplesProperties.getBiosamplesWebappSampletabUri();
    }

    @ModelAttribute("usiUrl")
    public URI addLinkUsi() {
        return bioSamplesProperties.getUsiCoreUri();
    }
}