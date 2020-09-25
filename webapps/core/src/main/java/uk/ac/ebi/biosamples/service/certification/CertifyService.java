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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.certification.*;

@Service
public class CertifyService {
  private Identifier identifier;
  private Interrogator interrogator;
  private Curator curator;
  private Certifier certifier;
  private Recorder recorder;
  private ConfigLoader configLoader;

  public CertifyService(
      Certifier certifier,
      Curator curator,
      Identifier identifier,
      Interrogator interrogator,
      @Qualifier("nullRecorder") Recorder recorder,
      ConfigLoader configLoader) {
    this.certifier = certifier;
    this.curator = curator;
    this.identifier = identifier;
    this.interrogator = interrogator;
    this.recorder = recorder;
    this.configLoader = configLoader;
  }

  public List<Certificate> certify(String data, boolean isJustCertification) {
    Set<CertificationResult> certificationResults = new LinkedHashSet<>();
    SampleDocument rawSampleDocument = identifier.identify(data);
    certificationResults.add(certifier.certify(rawSampleDocument, isJustCertification));
    List<Certificate> certificates = new ArrayList<>();

    certificationResults.forEach(
        certificationResult ->
            certificationResult
                .getCertificates()
                .forEach(
                    certificate -> {
                      final Checklist checklist = certificate.getChecklist();

                      final Certificate cert =
                          Certificate.build(
                              checklist.getName(), checklist.getVersion(), checklist.getFileName());
                      certificates.add(cert);
                    }));

    return certificates;
  }

  public BioSamplesCertificationComplainceResult recordResult(
      String data, boolean isJustCertification) {
    Set<CertificationResult> certificationResults = new LinkedHashSet<>();
    SampleDocument rawSampleDocument = identifier.identify(data);
    return doRecordResult(isJustCertification, certificationResults, rawSampleDocument);
  }

  public BioSamplesCertificationComplainceResult recordResult(
      SampleDocument rawSampleDocument, boolean isJustCertification) {
    Set<CertificationResult> certificationResults = new LinkedHashSet<>();
    return doRecordResult(isJustCertification, certificationResults, rawSampleDocument);
  }

  private BioSamplesCertificationComplainceResult doRecordResult(
      boolean isJustCertification,
      Set<CertificationResult> certificationResults,
      SampleDocument rawSampleDocument) {
    certificationResults.add(certifier.certify(rawSampleDocument, isJustCertification));

    InterrogationResult interrogationResult = interrogator.interrogate(rawSampleDocument);

    List<PlanResult> planResults = curator.runCurationPlans(interrogationResult);

    for (PlanResult planResult : planResults) {
      if (planResult.curationsMade()) {
        certificationResults.add(certifier.certify(planResult, isJustCertification));
      }
    }

    List<Recommendation> recommendations = curator.runRecommendations(interrogationResult);

    return recorder.record(certificationResults, recommendations);
  }

  public String getCertificateFileContentByCertificateName(String certificateName)
      throws IOException {
    final List<Checklist> checklists =
        configLoader.config.getChecklists().stream()
            .filter(checklist -> checklist.getName().equals(certificateName))
            .collect(Collectors.toList());
    String fileName = null;

    if (checklists.size() > 0) {
      fileName = checklists.get(0).getFileName();
    }

    if (fileName != null && !fileName.isEmpty()) {
      try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
        String jsonData = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());

        return jsonData;
      }
    }

    return "";
  }

  public String getCertificateFileNameByCertificateName(String certificateName) throws IOException {
    final List<Checklist> checklists =
        configLoader.config.getChecklists().stream()
            .filter(checklist -> checklist.getName().equals(certificateName))
            .collect(Collectors.toList());
    String fileName = null;

    if (checklists.size() > 0) {
      fileName = checklists.get(0).getFileName();
    }

    if (fileName != null && !fileName.isEmpty()) {
      return fileName;
    }

    return "";
  }
}
