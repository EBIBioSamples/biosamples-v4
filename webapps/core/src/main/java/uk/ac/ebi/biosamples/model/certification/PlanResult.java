package uk.ac.ebi.biosamples.model.certification;

import java.util.ArrayList;
import java.util.List;

public class PlanResult implements HasCuratedSample, HasPlan {
    private final SampleDocument sampleDocument;
    private final Plan plan;
    private final List<CurationResult> curationResults;

    public PlanResult(SampleDocument sampleDocument, Plan plan) {
        this.sampleDocument = sampleDocument;
        this.plan = plan;
        this.curationResults = new ArrayList<>();
    }

    public void addCurationResult(CurationResult curationResult) {
        curationResults.add(curationResult);
    }

    public List<CurationResult> getCurationResults() {
        return curationResults;
    }

    public boolean curationsMade() {
        for (CurationResult curationResult : curationResults) {
            if (curationResult.isApplied()) {
                return true;
            }
        }
        return false;
    }

    public SampleDocument getSampleDocument() {
        return sampleDocument;
    }

    public Plan getPlan() {
        return plan;
    }

    @Override
    public String toString() {
        return "PlanResult{" +
                "sampleDocument=" + sampleDocument +
                ", plan=" + plan +
                '}';
    }
}
