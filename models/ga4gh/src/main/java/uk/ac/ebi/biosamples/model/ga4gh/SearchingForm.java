package uk.ac.ebi.biosamples.model.ga4gh;

import org.springframework.stereotype.Component;

@Component
public class SearchingForm {

    private String text;
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }




}
