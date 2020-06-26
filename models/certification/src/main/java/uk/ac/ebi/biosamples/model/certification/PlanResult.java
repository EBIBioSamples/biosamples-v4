package uk.ac.ebi.biosamples.model.certification;

import java.util.ArrayList;
import java.util.List;

public class PlanResult implements HasCuratedSample, HasPlan {

    private final Sample sample;

    private final Plan plan;

    private final List<CurationResult> curationResults;

    public PlanResult(Sample sample, Plan plan) {
        this.sample = sample;
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

    public Sample getSample() {
        return sample;
    }

    public Plan getPlan() {
        return plan;
    }

    @Override
    public String toString() {
        return "PlanResult{" +
                "sample=" + sample +
                ", plan=" + plan +
                '}';
    }
}
