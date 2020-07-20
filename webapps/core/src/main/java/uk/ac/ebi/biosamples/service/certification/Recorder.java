package uk.ac.ebi.biosamples.service.certification;

import uk.ac.ebi.biosamples.model.certification.CertificationResult;
import uk.ac.ebi.biosamples.model.certification.RecorderResult;

import java.util.Set;

public interface Recorder {
    RecorderResult record(Set<CertificationResult> certificationResult);
}
