package uk.ac.ebi.biosamples.model.certification;

import java.util.List;

public class InterrogationResult {

    private final Sample sample;
    private final List<Checklist> checklists;

    public InterrogationResult(Sample sample, List<Checklist> checklists) {
        this.sample = sample;
        this.checklists = checklists;
    }

    public Sample getSample() {
        return sample;
    }

    public List<Checklist> getChecklists() {
        return checklists;
    }

    @Override
    public String toString() {
        return "InterrogationResult{" +
                "sample=" + sample +
                ", checklists=" + checklists +
                '}';
    }
}
