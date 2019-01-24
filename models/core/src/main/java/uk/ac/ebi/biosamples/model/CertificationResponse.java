package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CertificationResponse {

    @JsonProperty("certificates")
    public List<Certificate> getCertificates() {
        return certificates;
    }

    private List<Certificate> certificates;

    public void setCertificates(List<Certificate> certificates) {
        this.certificates = certificates;
    }

    @Override
    public String toString() {
        return "CertificationResponse{" +
                "certificates=" + certificates +
                '}';
    }
}
