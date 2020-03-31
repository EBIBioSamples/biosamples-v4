package uk.ac.ebi.biosamples;

public class PipelineResult {
    private final long modifiedRecords;
    private final boolean success;
    private final String accession;

    public PipelineResult(String accession, long modifiedRecords, boolean success) {
        this.accession = accession;
        this.modifiedRecords = modifiedRecords;
        this.success = success;
    }

    public String getAccession() {
        return accession;
    }

    public long getModifiedRecords() {
        return modifiedRecords;
    }

    public boolean isSuccess() {
        return success;
    }
}
