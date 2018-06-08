package uk.ac.ebi.biosamples.exceptions;

public class UnexpectedSampleTabRelationshipException extends SampleTabException {
    public String sampleName;
    public String relationType;
    public String relationTarget;


    public UnexpectedSampleTabRelationshipException(String sampleName, String relationType, String relationTarget) {
        super(String.format("The %s column in the SampleTab for sample %s contains an unexpected target %s",
                relationType, sampleName, relationTarget));
        this.sampleName = sampleName;
        this.relationType = relationType;
        this.relationTarget = relationTarget;

    }
}
