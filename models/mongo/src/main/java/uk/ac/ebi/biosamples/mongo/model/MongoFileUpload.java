package uk.ac.ebi.biosamples.mongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.biosamples.mongo.util.BioSamplesFileUploadSubmissionStatus;

import java.util.List;

@Document(collection = "mongoFileUpload")
public class MongoFileUpload {
    @Id
    private final String xmlPayloadId;
    private final BioSamplesFileUploadSubmissionStatus submissionStatus;
    private final String submitterDetails;
    private final String checklist;
    private final boolean isWebin;
    private List<String> accessions;

    public MongoFileUpload(String xmlPayloadId, BioSamplesFileUploadSubmissionStatus submissionStatus, String submitterDetails, String checklist, boolean isWebin, List<String> accessions) {
        this.xmlPayloadId = xmlPayloadId;
        this.submissionStatus = submissionStatus;
        this.submitterDetails = submitterDetails;
        this.checklist = checklist;
        this.isWebin = isWebin;
        this.accessions = accessions;
    }

    public String getChecklist() {
        return checklist;
    }

    public String getXmlPayloadId() {
        return xmlPayloadId;
    }

    public BioSamplesFileUploadSubmissionStatus getSubmissionStatus() {
        return submissionStatus;
    }

    public String getSubmitterDetails() {
        return submitterDetails;
    }

    public boolean isWebin() {
        return isWebin;
    }

    public List<String> getAccessions() {
        return accessions;
    }
}
