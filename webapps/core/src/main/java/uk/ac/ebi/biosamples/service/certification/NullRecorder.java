package uk.ac.ebi.biosamples.service.certification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.Certificate;
import uk.ac.ebi.biosamples.model.certification.CertificationResult;
import uk.ac.ebi.biosamples.model.certification.BioSamplesCertificationComplainceResult;

import java.util.Set;

@Service
public class NullRecorder implements Recorder {
    private static Logger LOG = LoggerFactory.getLogger(NullRecorder.class);
    private static Logger EVENTS = LoggerFactory.getLogger("events");

    @Override
    public BioSamplesCertificationComplainceResult record(Set<CertificationResult> certificationResults) {
        BioSamplesCertificationComplainceResult bioSamplesCertificationComplainceResult = new BioSamplesCertificationComplainceResult();
        if (certificationResults == null) {
            throw new IllegalArgumentException("cannot record a null certification result");
        }
        
        for (CertificationResult certificationResult : certificationResults) {
            for (Certificate certificate : certificationResult.getCertificates()) {
                bioSamplesCertificationComplainceResult.add(certificate);
                EVENTS.info(String.format("%s recorded %s certificate", certificate.getSampleDocument().getAccession(), certificate.getChecklist().getID()));
            }
        }

        return bioSamplesCertificationComplainceResult;
    }
}
