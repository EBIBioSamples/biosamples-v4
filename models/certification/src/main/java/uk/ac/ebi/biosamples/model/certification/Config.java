package uk.ac.ebi.biosamples.model.certification;

import java.util.List;

public class Config {

    private List<Checklist> checklists;

    private List<Plan> plans;

    public List<Checklist> getChecklists() {
        return checklists;
    }

    public void setChecklists(List<Checklist> checklists) {
        this.checklists = checklists;
    }

    public List<Plan> getPlans() {
        return plans;
    }

    public void setPlans(List<Plan> plans) {
        this.plans = plans;
    }

    public Config() {
    }
}


