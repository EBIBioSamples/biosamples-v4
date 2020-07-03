package uk.ac.ebi.biosamples.service.certification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.certification.*;
import uk.ac.ebi.biosamples.service.CurationPersistService;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CertifyService {
    @Autowired
    private CurationPersistService curationPersistService;

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

    public RecorderResult run(String data) {
        Set<CertificationResult> certificationResults = new LinkedHashSet<>();
        SampleDocument rawSampleDocument = identifier.identify(data);
        certificationResults.add(certifier.certify(rawSampleDocument));
        List<PlanResult> planResults = curator.runCurationPlans(interrogator.interrogate(rawSampleDocument));
        for (PlanResult planResult : planResults) {
            if (planResult.curationsMade()) {
                certificationResults.add(certifier.certify(planResult));
            }
        }

        certificationResults.forEach(certificationResult -> {
            certificationResult.getCertificates().forEach(certificate -> {
                final Checklist checklist = certificate.getChecklist();

                final Attribute attribute = Attribute.build("certificate", checklist.getName() + " " + checklist.getVersion());
                final uk.ac.ebi.biosamples.model.Curation curation = Curation.build(null, Collections.singleton(attribute), null, null);
                final CurationLink curationLink = CurationLink.build(rawSampleDocument.getAccession(), curation, "self.BiosampleImportNCBI", Instant.now());

                curationPersistService.store(curationLink);

            });
        });

        return recorder.record(certificationResults);
    }
}
