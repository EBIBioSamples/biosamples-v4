/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service.certification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.BioSamplesCertificationComplainceResult;
import uk.ac.ebi.biosamples.model.certification.Certificate;
import uk.ac.ebi.biosamples.model.certification.CertificationResult;

@Service
public class FileRecorder implements Recorder {
  private static Logger LOG = LoggerFactory.getLogger(FileRecorder.class);
  private static Logger EVENTS = LoggerFactory.getLogger("events");

  @Override
  public BioSamplesCertificationComplainceResult record(
      Set<CertificationResult> certificationResults) {
    BioSamplesCertificationComplainceResult bioSamplesCertificationComplainceResult =
        new BioSamplesCertificationComplainceResult();
    if (certificationResults == null) {
      throw new IllegalArgumentException("cannot record a null certification result");
    }

    for (CertificationResult certificationResult : certificationResults) {
      for (Certificate certificate : certificationResult.getCertificates()) {
        EVENTS.info(
            String.format(
                "%s recorded %s certificate",
                certificate.getSampleDocument().getAccession(),
                certificate.getChecklist().getID()));
        bioSamplesCertificationComplainceResult.add(certificate);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String fileName =
            certificate.getSampleDocument().getAccession()
                + "-"
                + certificate.getSampleDocument().getHash()
                + "-certification.json";
        try {
          objectMapper.writeValue(new File(fileName), certificationResult);
        } catch (IOException e) {
          LOG.error(String.format("failed to write file for %s", fileName));
        }
      }
    }

    return bioSamplesCertificationComplainceResult;
  }
}
