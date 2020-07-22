package uk.ac.ebi.biosamples.service.certification;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.certification.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CertifyService {
    private Identifier identifier;
    private Interrogator interrogator;
    private Curator curator;
    private Certifier certifier;
    private Recorder recorder;

    public CertifyService(Certifier certifier, Curator curator, Identifier identifier, Interrogator interrogator, @Qualifier("nullRecorder") Recorder recorder) {
        this.certifier = certifier;
        this.curator = curator;
        this.identifier = identifier;
        this.interrogator = interrogator;
        this.recorder = recorder;
    }

    public List<Certificate> certify(String data) {
        Set<CertificationResult> certificationResults = new LinkedHashSet<>();
        SampleDocument rawSampleDocument = identifier.identify(data);
        certificationResults.add(certifier.certify(rawSampleDocument));
        List<Certificate> certificates = new ArrayList<>();

        certificationResults.forEach(certificationResult -> {
            certificationResult.getCertificates().forEach(certificate -> {
                final Checklist checklist = certificate.getChecklist();

                final Certificate cert = Certificate.build(checklist.getName(), checklist.getVersion(), checklist.getFileName());
                certificates.add(cert);
            });
        });

        return certificates;
    }

    public BioSamplesCertificationComplainceResult recordResult(String data) {
        Set<CertificationResult> certificationResults = new LinkedHashSet<>();
        SampleDocument rawSampleDocument = identifier.identify(data);
        certificationResults.add(certifier.certify(rawSampleDocument));
        List<PlanResult> planResults = curator.runCurationPlans(interrogator.interrogate(rawSampleDocument));

        for (PlanResult planResult : planResults) {
            if (planResult.curationsMade()) {
                certificationResults.add(certifier.certify(planResult));
            }
        }

        return recorder.record(certificationResults);
    }
}
