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
package uk.ac.ebi.biosamples.auth.services;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SubmittedViaType;
import uk.ac.ebi.biosamples.core.model.filter.AuthenticationFilter;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoAuthChangeRecord;
import uk.ac.ebi.biosamples.mongo.repository.MongoAuthChangeRepository;

@Service
@Slf4j
public class SamplesCrawlerAuthChangeHandler {
  private static final String WEBIN_ID_TO_CHANGE_TO = "Webin-69232";
  private final BioSamplesClient bioSamplesClient;
  private final MongoAuthChangeRepository mongoAuthChangeRepository;

  public SamplesCrawlerAuthChangeHandler(
      final BioSamplesClient bioSamplesClient,
      final MongoAuthChangeRepository mongoAuthChangeRepository) {
    this.bioSamplesClient = bioSamplesClient;
    this.mongoAuthChangeRepository = mongoAuthChangeRepository;
  }

  public Iterable<EntityModel<Sample>> getSamples(final String domain) {
    final Filter authenticationFilter = new AuthenticationFilter.Builder(domain).build();
    return bioSamplesClient.fetchSampleResourceAllWithoutCuration(
        "", List.of(authenticationFilter));
  }

  public void handleAuth(final EntityModel<Sample> sampleEntityModel, final String domain) {
    final Sample sample = sampleEntityModel.getContent();

    log.info("Handling Sample {}", sample.getAccession());

    final String accession = sample.getAccession();
    final String sampleDomain = sample.getDomain();
    final String webinId = sample.getWebinSubmissionAccountId();

    if (!accession.startsWith("SAME")
        || (sample.getSubmittedVia() == SubmittedViaType.PIPELINE_IMPORT
            || sample.getSubmittedVia() == SubmittedViaType.WEBIN_SERVICES)) {
      log.info("Sample is an imported sample: {} please update at source", accession);

      return;
    }

    if (sampleDomain != null && sampleDomain.equals(domain) && webinId == null) {
      log.info(
          "Sample authority needs to change for: "
              + accession
              + " setting to: "
              + WEBIN_ID_TO_CHANGE_TO);

      final Sample updatedSample =
          Sample.Builder.fromSample(sample)
              .withWebinSubmissionAccountId(WEBIN_ID_TO_CHANGE_TO)
              .withNoDomain()
              .build();
      final EntityModel<Sample> savedSampleEntityModel =
          bioSamplesClient.persistSampleResource(updatedSample);

      if (Objects.requireNonNull(savedSampleEntityModel.getContent())
          .getWebinSubmissionAccountId()
          .equals(WEBIN_ID_TO_CHANGE_TO)) {
        log.info("Sample " + accession + " updated");

        mongoAuthChangeRepository.save(
            new MongoAuthChangeRecord(accession, domain, WEBIN_ID_TO_CHANGE_TO));
      } else {
        log.info("Sample " + accession + " failed to be updated");
      }
    } else {
      log.info("Sample from some other domain, no change required");
    }
  }
}
