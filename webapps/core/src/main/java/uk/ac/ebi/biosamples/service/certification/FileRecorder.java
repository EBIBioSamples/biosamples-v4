/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service.certification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.BioSamplesCertificationComplainceResult;
import uk.ac.ebi.biosamples.model.certification.Certificate;
import uk.ac.ebi.biosamples.model.certification.CertificationResult;
import uk.ac.ebi.biosamples.model.certification.Recommendation;

@Service
public class FileRecorder implements Recorder {
  private static final Logger LOG = LoggerFactory.getLogger(FileRecorder.class);
  private static final Logger EVENTS = LoggerFactory.getLogger("events");

  @Override
  public BioSamplesCertificationComplainceResult record(
      final Set<CertificationResult> certificationResults,
      final List<Recommendation> recommendations) {
    final BioSamplesCertificationComplainceResult bioSamplesCertificationComplainceResult =
        new BioSamplesCertificationComplainceResult();
    final ObjectMapper objectMapper = new ObjectMapper();

    if (certificationResults == null) {
      throw new IllegalArgumentException("cannot record a null certification result");
    }

    for (final CertificationResult certificationResult : certificationResults) {
      for (final Certificate certificate : certificationResult.getCertificates()) {
        EVENTS.info(
            String.format(
                "%s recorded %s certificate",
                certificate.getSampleDocument().getAccession(),
                certificate.getChecklist().getID()));
        bioSamplesCertificationComplainceResult.add(certificate);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
          objectMapper.writeValueAsString(certificationResult);
        } catch (final IOException e) {
          LOG.error(String.format("failed to write"));
        }
      }
    }

    for (final Recommendation recommendation : recommendations) {
      bioSamplesCertificationComplainceResult.add(recommendation);
      try {
        objectMapper.writeValueAsString(recommendation);
      } catch (final JsonProcessingException e) {
        LOG.error(String.format("failed to write"));
      }
    }

    return bioSamplesCertificationComplainceResult;
  }
}
