package uk.ac.ebi.biosamples.model.certification;

import java.util.List;

public class InterrogationResult {
    private final SampleDocument sampleDocument;
    private final List<Checklist> checklists;

    public InterrogationResult(SampleDocument sampleDocument, List<Checklist> checklists) {
        this.sampleDocument = sampleDocument;
        this.checklists = checklists;
    }

    public SampleDocument getSampleDocument() {
        return sampleDocument;
    }

    public List<Checklist> getChecklists() {
        return checklists;
    }

    @Override
    public String toString() {
        return "InterrogationResult{" +
                "sampleDocument=" + sampleDocument +
                ", checklists=" + checklists +
                '}';
    }
}
