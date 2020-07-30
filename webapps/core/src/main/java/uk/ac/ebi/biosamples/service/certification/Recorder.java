package uk.ac.ebi.biosamples.service.certification;

import uk.ac.ebi.biosamples.model.certification.CertificationResult;
import uk.ac.ebi.biosamples.model.certification.BioSamplesCertificationComplainceResult;

import java.util.Set;

public interface Recorder {
    BioSamplesCertificationComplainceResult record(Set<CertificationResult> certificationResult);
}
