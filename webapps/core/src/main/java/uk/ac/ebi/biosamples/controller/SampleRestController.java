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
package uk.ac.ebi.biosamples.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.SampleManipulationService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.WebinAuthenticationService;
import uk.ac.ebi.biosamples.service.taxonomy.TaxonomyClientService;
import uk.ac.ebi.biosamples.utils.LinkUtils;
import uk.ac.ebi.biosamples.utils.phenopacket.PhenopacketConverter;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

/**
 * Primary controller for REST operations both in JSON and XML and both read and write.
 *
 * <p>See {@link SampleHtmlController} for the HTML equivalent controller.
 *
 * @author faulcon
 */
@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples/{accession}")
@CrossOrigin
public class SampleRestController {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final SampleService sampleService;
  private final WebinAuthenticationService webinAuthenticationService;
  private final SampleManipulationService sampleManipulationService;
  private final SampleResourceAssembler sampleResourceAssembler;
  private final PhenopacketConverter phenopacketConverter;
  private final SchemaValidationService schemaValidationService;
  private final TaxonomyClientService taxonomyClientService;

  public SampleRestController(
      final SampleService sampleService,
      final WebinAuthenticationService webinAuthenticationService,
      final SampleManipulationService sampleManipulationService,
      final SampleResourceAssembler sampleResourceAssembler,
      final PhenopacketConverter phenopacketConverter,
      final SchemaValidationService schemaValidationService,
      final TaxonomyClientService taxonomyClientService) {
    this.sampleService = sampleService;
    this.webinAuthenticationService = webinAuthenticationService;
    this.sampleManipulationService = sampleManipulationService;
    this.sampleResourceAssembler = sampleResourceAssembler;
    this.phenopacketConverter = phenopacketConverter;
    this.schemaValidationService = schemaValidationService;
    this.taxonomyClientService = taxonomyClientService;
  }

  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public EntityModel<Sample> getSampleHal(
      @PathVariable final String accession,
      @RequestParam(name = "legacydetails", required = false) final String legacydetails,
      @RequestParam(name = "curationdomain", required = false) final String[] curationdomain) {
    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);
    final Optional<List<String>> decodedCurationDomains =
        LinkUtils.decodeTextsToArray(curationdomain);
    final Optional<Boolean> decodedLegacyDetails =
        "true".equals(legacydetails) ? Optional.of(Boolean.TRUE) : Optional.empty();
    final Optional<Sample> sample = sampleService.fetch(accession, decodedCurationDomains);

    sample.ifPresent(
        s -> {
          webinAuthenticationService.isSampleAccessible(s, principle);

          decodedLegacyDetails.ifPresent(d -> sampleManipulationService.removeLegacyFields(s));
        });

    return sample
        .map(s -> sampleResourceAssembler.toModel(s, decodedLegacyDetails, decodedCurationDomains))
        .orElseThrow(GlobalExceptions.SampleNotFoundException::new);
  }

  @RequestMapping(produces = "application/phenopacket+json")
  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping()
  public String getSamplePhenopacket(
      @PathVariable final String accession,
      @RequestParam(name = "legacydetails", required = false) final String legacydetails,
      @RequestParam(name = "curationdomain", required = false) final String[] curationdomain) {
    final Optional<List<String>> decodedCurationDomains =
        LinkUtils.decodeTextsToArray(curationdomain);
    final Optional<Boolean> decodedLegacyDetails =
        Optional.ofNullable("true".equals(legacydetails) ? Boolean.TRUE : null);
    final Optional<Sample> sample = sampleService.fetch(accession, decodedCurationDomains);

    return sample
        .map(
            s -> {
              webinAuthenticationService.isSampleAccessible(s, null);
              decodedLegacyDetails.ifPresent(d -> sampleManipulationService.removeLegacyFields(s));

              return phenopacketConverter.convertToJsonPhenopacket(s);
            })
        .orElseThrow(GlobalExceptions.SampleNotFoundException::new);
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public EntityModel<Sample> put(
      @PathVariable final String accession,
      @RequestBody Sample sample,
      @RequestParam(name = "setfulldetails", required = false, defaultValue = "true")
          final boolean setFullDetails) {
    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);

    if (principle == null) {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }

    validateRequest(sample, accession);

    log.debug("Received PUT request for accession: {}", accession);

    // Determine if user is a super user
    final boolean isWebinSuperUser = webinAuthenticationService.isWebinSuperUser(principle);

    // Check if sample accession exists for non-super users
    if (sampleService.isNotExistingAccession(accession) && !isWebinSuperUser) {
      throw new GlobalExceptions.SampleAccessionDoesNotExistException();
    }

    // Fetch existing sample
    final Optional<Sample> oldSample = sampleService.fetch(sample.getAccession(), Optional.empty());

    // Handle Webin submission and abstract data
    sample = webinAuthenticationService.handleWebinUserSubmission(sample, principle, oldSample);
    sample = handleAbstractDataForWebinSubmission(sample, principle);

    // Update and validate sample
    sample =
        Sample.Builder.fromSample(sample)
            .withUpdate(Instant.now())
            .withSubmittedVia(
                Optional.ofNullable(sample.getSubmittedVia()).orElse(SubmittedViaType.JSON_API))
            .build();
    sample = validateSample(sample, isWebinSuperUser);

    // Optionally remove legacy fields
    if (!setFullDetails) {
      log.trace("Removing legacy fields for accession: {}", accession);
      sample = sampleManipulationService.removeLegacyFields(sample);
    }

    // Persist sample and return assembled response
    sample =
        sampleService.persistSample(
            sample, oldSample.orElse(null), AuthorizationProvider.WEBIN, isWebinSuperUser);
    return sampleResourceAssembler.toModel(sample);
  }

  private Sample handleAbstractDataForWebinSubmission(
      Sample sample, final String webinIdFromAuthToken) {
    final SortedSet<AbstractData> abstractData = sample.getData();

    if (abstractData != null && !abstractData.isEmpty()) {
      if (webinAuthenticationService.isSampleSubmitter(sample, webinIdFromAuthToken)) {
        sample = Sample.Builder.fromSample(sample).build();
      } else {
        sample = Sample.Builder.fromSample(sample).withNoData().build();
      }
    }

    return sample;
  }

  private void validateRequest(final Sample sample, final String accession) {
    if (sample == null) {
      throw new RuntimeException("No sample provided");
    }

    if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
      throw new GlobalExceptions.SampleAccessionMismatchException();
    }
  }

  private Sample validateSample(Sample sample, final boolean isWebinSuperUser) {
    // Dont validate superuser samples, this helps to submit external (eg. NCBI, ENA) samples
    if (!isWebinSuperUser) {
      schemaValidationService.validate(sample);
      sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, true);
    }

    if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      schemaValidationService.validate(sample);
    }

    return sample;
  }
}
