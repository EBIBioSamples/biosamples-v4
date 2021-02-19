package uk.ac.ebi.biosamples.model.upload.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ValidationResult {
    private List<String> validationMessagesList = new ArrayList<>();

    public void addValidationMessage(String message) {
        validationMessagesList.add(message);
    }

    public List<String> getValidationMessagesList() {
        return validationMessagesList;
    }

    public void clear() {
        validationMessagesList.clear();
    }
}
