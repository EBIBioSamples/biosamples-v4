package uk.ac.ebi.biosamples.ga4ghmetadata;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import java.util.Date;

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
