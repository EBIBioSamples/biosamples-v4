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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.certification.*;

@Service
public class CertifyService {
  private final Identifier identifier;
  private final Interrogator interrogator;
  private final Curator curator;
  private final Certifier certifier;
  private final Recorder recorder;
  private final ConfigLoader configLoader;

  public CertifyService(
      final Certifier certifier,
      final Curator curator,
      final Identifier identifier,
      final Interrogator interrogator,
      @Qualifier("nullRecorder") final Recorder recorder,
      final ConfigLoader configLoader) {
    this.certifier = certifier;
    this.curator = curator;
    this.identifier = identifier;
    this.interrogator = interrogator;
    this.recorder = recorder;
    this.configLoader = configLoader;
  }

  public List<Certificate> certify(final String data, final boolean isJustCertification) {
    final Set<CertificationResult> certificationResults = new LinkedHashSet<>();
    final SampleDocument rawSampleDocument = identifier.identify(data);
    certificationResults.add(certifier.certify(rawSampleDocument, isJustCertification));
    return getCertificatesFromCertificationResults(certificationResults);
  }

  public List<Certificate> certify(
      final String data, final boolean isJustCertification, final String inputChecklist) {
    final Set<CertificationResult> certificationResults = new LinkedHashSet<>();
    final SampleDocument rawSampleDocument = identifier.identify(data);
    certificationResults.add(
        certifier.certify(rawSampleDocument, isJustCertification, inputChecklist));
    return getCertificatesFromCertificationResults(certificationResults);
  }

  private List<Certificate> getCertificatesFromCertificationResults(
      final Set<CertificationResult> certificationResults) {
    final List<Certificate> certificates = new ArrayList<>();

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
      final String data, final boolean isJustCertification) {
    final Set<CertificationResult> certificationResults = new LinkedHashSet<>();
    final SampleDocument rawSampleDocument = identifier.identify(data);
    return doRecordResult(isJustCertification, certificationResults, rawSampleDocument);
  }

  public BioSamplesCertificationComplainceResult recordResult(
      final SampleDocument rawSampleDocument, final boolean isJustCertification) {
    final Set<CertificationResult> certificationResults = new LinkedHashSet<>();
    return doRecordResult(isJustCertification, certificationResults, rawSampleDocument);
  }

  private BioSamplesCertificationComplainceResult doRecordResult(
      final boolean isJustCertification,
      final Set<CertificationResult> certificationResults,
      final SampleDocument rawSampleDocument) {
    certificationResults.add(certifier.certify(rawSampleDocument, isJustCertification));

    final InterrogationResult interrogationResult = interrogator.interrogate(rawSampleDocument);

    final List<PlanResult> planResults = curator.runCurationPlans(interrogationResult);

    for (final PlanResult planResult : planResults) {
      if (planResult.curationsMade()) {
        certificationResults.add(certifier.certify(planResult, isJustCertification));
      }
    }

    final List<Recommendation> recommendations = curator.runRecommendations(interrogationResult);

    return recorder.record(certificationResults, recommendations);
  }

  public String getCertificateByCertificateName(final String certificateName) throws IOException {
    final Optional<Checklist> matchedChecklist = getChecklistByCertificateName(certificateName);
    String fileName = null;

    if (matchedChecklist.isPresent()) {
      fileName = matchedChecklist.get().getFileName();
    }

    if (fileName != null && !fileName.isEmpty()) {
      try (final InputStream inputStream =
          getClass().getClassLoader().getResourceAsStream(fileName)) {
        final String jsonData = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());

        return jsonData;
      }
    }

    return "";
  }

  public String getCertificateFileNameByCertificateName(final String certificateName) {
    final Optional<Checklist> matchedChecklist = getChecklistByCertificateName(certificateName);
    String fileName = null;

    if (matchedChecklist.isPresent()) {
      fileName = matchedChecklist.get().getFileName();
    }

    if (fileName != null && !fileName.isEmpty()) {
      return fileName.substring(fileName.lastIndexOf("/") + 1);
    }

    return "";
  }

  private Optional<Checklist> getChecklistByCertificateName(final String certificateName) {
    return configLoader.config.getChecklists().stream()
        .filter(checklist -> checklist.getName().equals(certificateName))
        .findFirst();
  }

  public List<String> getAllCertificates() {
    return configLoader.config.getChecklists().stream()
        .map(
            checklist -> {
              try {
                return IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream(checklist.getFileName()),
                    StandardCharsets.UTF_8.name());
              } catch (final IOException e) {
                e.printStackTrace();
              }

              return null;
            })
        .collect(Collectors.toList());
  }

  public List<String> getAllCertificateNames() {
    return configLoader.config.getChecklists().stream()
        .map(checklist -> checklist.getName() /*+ "-" + checklist.getVersion()*/)
        .collect(Collectors.toList());
  }
}
