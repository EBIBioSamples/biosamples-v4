package uk.ac.ebi.biosamples.service.certification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.Certificate;
import uk.ac.ebi.biosamples.model.certification.CertificationResult;
import uk.ac.ebi.biosamples.model.certification.RecorderResult;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@Service
public class FileRecorder implements Recorder {

    private static Logger LOG = LoggerFactory.getLogger(FileRecorder.class);
    private static Logger EVENTS = LoggerFactory.getLogger("events");

    @Override
    public RecorderResult record(Set<CertificationResult> certificationResults) {
        RecorderResult recorderResult = new RecorderResult();
        if (certificationResults == null) {
            throw new IllegalArgumentException("cannot record a null certification result");
        }
        for (CertificationResult certificationResult : certificationResults) {
            for (Certificate certificate : certificationResult.getCertificates()) {
                EVENTS.info(String.format("%s recorded %s certificate", certificate.getSampleDocument().getAccession(), certificate.getChecklist().getID()));
                recorderResult.add(certificate);
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                String fileName = certificate.getSampleDocument().getAccession() + "-" + certificate.getSampleDocument().getHash() + "-certification.json";
                try {
                    objectMapper.writeValue(new File(fileName), certificationResult);
                } catch (IOException e) {
                    LOG.error(String.format("failed to write file for %s", fileName));
                }
            }
        }
        return recorderResult;
    }
}
