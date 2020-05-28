package uk.ac.ebi.biosamples.model;

public class ClearinghouseCurations {
    private String assertionMethod;
    private String attributePost;
    private String recordType;
    private String valuePost;
    private AssertionEvidences[] assertionEvidences;
    private String updatedTimestamp;
    private String providerUrl;
    private String attributePre;
    private String recordId;
    private String attributeDelete;
    private String submittedTimestamp;
    private String id;
    private String suppressed;
    private String providerName;

    public String getAssertionMethod() {
        return assertionMethod;
    }

    public void setAssertionMethod(String assertionMethod) {
        this.assertionMethod = assertionMethod;
    }

    public String getAttributePost() {
        return attributePost;
    }

    public void setAttributePost(String attributePost) {
        this.attributePost = attributePost;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getValuePost() {
        return valuePost;
    }

    public void setValuePost(String valuePost) {
        this.valuePost = valuePost;
    }

    public AssertionEvidences[] getAssertionEvidences() {
        return assertionEvidences;
    }

    public void setAssertionEvidences(AssertionEvidences[] assertionEvidences) {
        this.assertionEvidences = assertionEvidences;
    }

    public String getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    public void setUpdatedTimestamp(String updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }

    public String getAttributePre() {
        return attributePre;
    }

    public void setAttributePre(String attributePre) {
        this.attributePre = attributePre;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getAttributeDelete() {
        return attributeDelete;
    }

    public void setAttributeDelete(String attributeDelete) {
        this.attributeDelete = attributeDelete;
    }

    public String getSubmittedTimestamp() {
        return submittedTimestamp;
    }

    public void setSubmittedTimestamp(String submittedTimestamp) {
        this.submittedTimestamp = submittedTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSuppressed() {
        return suppressed;
    }

    public void setSuppressed(String suppressed) {
        this.suppressed = suppressed;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    @Override
    public String toString() {
        return "ClearinghouseCurations [assertionMethod = " + assertionMethod + ", attributePost = " + attributePost + ", recordType = " + recordType + ", valuePost = " + valuePost + ", assertionEvidences = " + assertionEvidences + ", updatedTimestamp = " + updatedTimestamp + ", providerUrl = " + providerUrl + ", attributePre = " + attributePre + ", recordId = " + recordId + ", attributeDelete = " + attributeDelete + ", submittedTimestamp = " + submittedTimestamp + ", id = " + id + ", suppressed = " + suppressed + ", providerName = " + providerName + "]";
    }
}



