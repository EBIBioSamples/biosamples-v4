package uk.ac.ebi.biosamples.model.certification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Objects;

public class Certificate implements HasCuratedSample, HasChecklist {

    private final Sample sample;

    private final List<CurationResult> curationsResults;

    private final Checklist checklist;

    @JsonPropertyOrder({"sample", "curationsResults", "checklist"})
    public Certificate(HasCuratedSample hasCuratedSample, Checklist checklist) {
        this(hasCuratedSample.getSample(), hasCuratedSample.getCurationResults(), checklist);
    }

    @JsonPropertyOrder({"sample", "curationsResults", "checklist"})
    public Certificate(Sample sample, List<CurationResult> curationsResults, Checklist checklist) {
        this.sample = sample;
        this.curationsResults = curationsResults;
        this.checklist = checklist;
    }

    public Sample getSample() {
        return sample;
    }

    public Checklist getChecklist() {
        return checklist;
    }

    @JsonProperty(value = "curations")
    public List<CurationResult> getCurationResults() {
        return curationsResults;
    }

    @Override
    public String toString() {
        return "Certificate{" +
                "sample=" + sample +
                ", curationsResults=" + curationsResults +
                ", checklist=" + checklist +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Certificate that = (Certificate) o;
        return sample.equals(that.sample) &&
                curationsResults.equals(that.curationsResults) &&
                checklist.equals(that.checklist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sample, curationsResults, checklist);
    }
}
