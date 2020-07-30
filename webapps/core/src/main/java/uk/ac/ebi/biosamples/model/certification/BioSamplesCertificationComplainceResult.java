package uk.ac.ebi.biosamples.model.certification;

import java.util.ArrayList;
import java.util.List;

public class BioSamplesCertificationComplainceResult {
    private final List<Certificate> certificates = new ArrayList<>();

    public List<Certificate> getCertificates() {
        return certificates;
    }

    public void add(Certificate certificate) {
        certificates.add(certificate);
    }

    @Override
    public String toString() {
        return "BioSamplesCertificationComplianceResult{" +
                "certificates=" + certificates +
                '}';
    }
}
