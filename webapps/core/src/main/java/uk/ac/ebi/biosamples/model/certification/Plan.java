package uk.ac.ebi.biosamples.model.certification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Plan {
    @JsonProperty(value = "candidate_checklist_id")
    private String candidateChecklistID;
    @JsonProperty(value = "certification_checklist_id")
    private String certificationChecklistID;

    private List<Curation> curations;

    public Plan(String candidateChecklistID, String certificationChecklistID, List<Curation> curations) {
        this.candidateChecklistID = candidateChecklistID;
        this.certificationChecklistID = certificationChecklistID;
        this.curations = curations;
    }

    private Plan() {

    }

    public String getID() {
        return String.format("%s -> %s", candidateChecklistID, certificationChecklistID);
    }

    public String getCandidateChecklistID() {
        return candidateChecklistID;
    }

    public void setCandidateChecklistID(String candidateChecklistID) {
        this.candidateChecklistID = candidateChecklistID;
    }

    public String getCertificationChecklistID() {
        return certificationChecklistID;
    }

    public void setCertificationChecklistID(String certificationChecklistID) {
        this.certificationChecklistID = certificationChecklistID;
    }

    public List<Curation> getCurations() {
        return curations;
    }

    public void setCurations(List<Curation> curations) {
        this.curations = curations;
    }

    public CurationResult applyCuration(SampleDocument sampleDocument, Curation curation) {
        JSONObject jsonObject = new JSONObject(sampleDocument.getDocument());
        JSONObject sampleCharacteristics = jsonObject.getJSONObject("characteristics");
        String targetCharateristic = curation.getCharacteristic();
        if (sampleCharacteristics.has(targetCharateristic)) {
            JSONArray jsonArray = sampleCharacteristics.getJSONArray(curation.getCharacteristic());
            String before = jsonArray.getJSONObject(0).getString("text");
            CurationResult curationResult = new CurationResult(targetCharateristic, before, curation.getValue());
            return curationResult;
        }
        return new CurationResult(targetCharateristic);
    }

    @Override
    public String toString() {
        return "Plan{" +
                "candidateChecklistID='" + candidateChecklistID + '\'' +
                ", certificationChecklistID='" + certificationChecklistID + '\'' +
                '}';
    }
}
