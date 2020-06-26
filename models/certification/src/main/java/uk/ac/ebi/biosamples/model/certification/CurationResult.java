package uk.ac.ebi.biosamples.model.certification;

public class CurationResult {

    private final String characteristic;

    private final String before;

    private final String after;

    private final boolean applied;

    public CurationResult(String characteristic) {
        this.applied = false;
        this.characteristic = characteristic;
        this.before = null;
        this.after = null;
    }

    public CurationResult(String characteristic, String before, String after) {
        this.applied = true;
        this.characteristic = characteristic;
        this.before = before;
        this.after = after;
    }

    public boolean isApplied() {
        return applied;
    }

    public String getCharacteristic() {
        return characteristic;
    }

    public String getBefore() {
        return before;
    }

    public String getAfter() {
        return after;
    }

    @Override
    public String toString() {
        return "CurationResult{" +
                "characteristic='" + characteristic + '\'' +
                ", before='" + before + '\'' +
                ", after='" + after + '\'' +
                ", applied=" + applied +
                '}';
    }
}
