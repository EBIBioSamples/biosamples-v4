package uk.ac.ebi.biosamples.model.ga4ghmetadata;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class SearchingForm {

    private String text;
    @DateTimeFormat(pattern = "yyyy-mm-dd")
    private Date releaseDateFrom;
    @DateTimeFormat(pattern = "yyyy-mm-dd")
    private Date releaseDateUntil;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getReleaseDateFrom() {
        return releaseDateFrom;
    }

    public void setReleaseDateFrom(Date releaseDateFrom) {
        this.releaseDateFrom = releaseDateFrom;
    }

    public Date getReleaseDateUntil() {
        return releaseDateUntil;
    }

    public void setReleaseDateUntil(Date releaseDateUntil) {
        this.releaseDateUntil = releaseDateUntil;
    }


}
