package uk.ac.ebi.biosamples.model.certification;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class CertificationResult {
    private final List<Certificate> certificates = new ArrayList<>();
    private String sampleAccession;

    public CertificationResult(String sampleAccession) {
        this.sampleAccession = sampleAccession;
    }

    private CertificationResult() {

    }

    @JsonIgnore
    public String getSampleAccession() {
        return sampleAccession;
    }

    public void add(Certificate certificate) {
        certificates.add(certificate);
    }

    public List<Certificate> getCertificates() {
        return certificates;
    }
}
