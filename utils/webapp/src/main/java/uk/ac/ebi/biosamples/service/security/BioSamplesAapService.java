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
package uk.ac.ebi.biosamples.service.security;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.security.UserAuthentication;

@Service
public class BioSamplesAapService {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final BioSamplesProperties bioSamplesProperties;
  private final SampleService sampleService;
  private final AapTokenService tokenService;
  private final AapDomainService domainService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;

  public BioSamplesAapService(
      RestTemplateBuilder restTemplateBuilder,
      BioSamplesProperties bioSamplesProperties,
      SampleService sampleService,
      AapTokenService tokenService,
      AapDomainService domainService,
      BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService) {
    Traverson traverson =
        new Traverson(bioSamplesProperties.getBiosamplesClientAapUri(), MediaTypes.HAL_JSON);
    this.tokenService = tokenService;
    this.domainService = domainService;
    traverson.setRestOperations(restTemplateBuilder.build());
    this.bioSamplesProperties = bioSamplesProperties;
    this.sampleService = sampleService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
  }

  public String authenticate(String userName, String password) {
    return tokenService.getAAPToken(userName, password);
  }

  public List<String> getDomains(String token) {
    domainService.getMyDomains(token).forEach(domain -> log.info(domain.getDomainName()));

    return domainService.getMyDomains(token).stream()
        .map(Domain::getDomainName)
        .collect(Collectors.toList());
  }

  /**
   * Returns a set of domains that the current user has access to (uses thread-bound spring
   * security) Always returns a set, even if its empty if not logged in
   */
  public Set<String> getDomains() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    log.trace("authentication = " + authentication);

    // not sure this can ever happen
    if (authentication == null) {
      return Collections.emptySet();

    } else if (authentication instanceof AnonymousAuthenticationToken) {
      return Collections.emptySet();
    } else if (authentication instanceof UserAuthentication) {

      UserAuthentication userAuthentication = (UserAuthentication) authentication;

      log.trace("userAuthentication = " + userAuthentication.getName());

      final Collection<? extends GrantedAuthority> authorities =
          userAuthentication.getAuthorities();

      log.trace("userAuthentication = " + authorities);
      log.trace("userAuthentication = " + userAuthentication.getPrincipal());
      log.trace("userAuthentication = " + userAuthentication.getCredentials());

      Set<String> domains = new HashSet<>();

      if (authorities == null) {
        return Collections.emptySet();
      }

      for (GrantedAuthority authority : authorities) {
        if (authority instanceof Domain) {
          log.trace("Found domain " + authority);
          Domain domain = (Domain) authority;

          log.trace("domain.getDomainName() = " + domain.getDomainName());
          log.trace("domain.getDomainReference() = " + domain.getDomainReference());

          // NOTE this should use reference, but that is not populated in the tokens at
          // the moment
          // domains.add(domain.getDomainReference());
          domains.add(domain.getDomainName());
        } else {
          log.warn(
              "Found non-domain GrantedAuthority "
                  + authority
                  + " for user "
                  + userAuthentication.getName());
        }
      }
      return domains;
    } else {
      return Collections.emptySet();
    }
  }

  /**
   * Function that checks if a sample has a domain the current user has access to, or if the user
   * only has a single domain sets the sample to that domain.
   *
   * <p>May return a different version of the sample, so return needs to be stored in future for
   * that sample.
   */
  public Sample handleSampleDomain(Sample sample, Optional<Sample> oldSample)
      throws GlobalExceptions.SampleNotAccessibleException,
          GlobalExceptions.DomainMissingException {
    // Get the domains the current user has access to
    final Set<String> usersDomains = getDomains();
    final String domain = sample.getDomain();

    if (oldSample.isPresent()
        && oldSample.get().getSubmittedVia()
            == SubmittedViaType.PIPELINE_IMPORT) { // pipeline imports access protection
      if (sample.getSubmittedVia() != SubmittedViaType.PIPELINE_IMPORT) {
        throw new GlobalExceptions.InvalidSubmissionSourceException();
      }
    }

    // check if FILE UPLOADER submission, domain changes are not allowed, handled differently
    if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      if (oldSample.isPresent() && !domain.equals(oldSample.get().getDomain())) {
        throw new GlobalExceptions.SampleDomainMismatchException();
      } else {
        return sample;
      }
    } else { // non FILE UPLOADER submissions
      if (domain == null || domain.length() == 0) {
        // if the sample doesn't have a domain, and the user has one domain, then they must be
        // submitting to that domain
        if (usersDomains.size() > 0) {
          sample =
              Sample.Builder.fromSample(sample)
                  .withDomain(usersDomains.iterator().next())
                  .withNoWebinSubmissionAccountId()
                  .build();
        } else {
          throw new GlobalExceptions.DomainMissingException();
        }
      }

      if (usersDomains.contains(bioSamplesProperties.getBiosamplesAapSuperWrite())) {
        // Super user submission
        return sample;
      } else if (usersDomains.contains(domain)) {
        return sample;
      } else {
        log.warn(
            "User asked to submit sample to domain "
                + domain
                + " but has access to "
                + usersDomains);
        throw new GlobalExceptions.SampleNotAccessibleException();
      }
    }
  }

  public void handleStructuredDataDomain(StructuredData structuredData) {
    final Set<String> usersDomains = getDomains();
    if (usersDomains.contains(bioSamplesProperties.getBiosamplesAapSuperWrite())) {
      return;
    }

    structuredData
        .getData()
        .forEach(
            data -> {
              if (data.getDomain() == null) {
                throw new GlobalExceptions.StructuredDataDomainMissingException();
              } else if (!usersDomains.contains(data.getDomain())) {
                throw new GlobalExceptions.SampleDomainMismatchException();
              }
            });
  }

  public boolean isStructuredDataSubmittedBySampleSubmitter(Sample sample)
      throws GlobalExceptions.StructuredDataNotAccessibleException,
          GlobalExceptions.StructuredDataDomainMissingException {
    // get the domains the current user has access to
    final Set<String> usersDomains = getDomains();
    final String sampleDomain = sample.getDomain();

    final AtomicBoolean isDomainValid = new AtomicBoolean(false);

    sample
        .getData()
        .forEach(
            data -> {
              if (data.getDataType() != null) {
                final String structuredDataDomain = data.getDomain();

                if (structuredDataDomain == null) {
                  throw new GlobalExceptions.StructuredDataDomainMissingException();
                } else if (usersDomains.contains(data.getDomain())
                    && usersDomains.contains(
                        sampleDomain)) { // if the structured data domain and the sample domain both
                  // in usersDomain
                  // then we can consider the data being submitted by original submitter
                  isDomainValid.set(true);
                }
              }
            });

    if (usersDomains.contains(bioSamplesProperties.getBiosamplesAapSuperWrite())) return true;
    else if (isDomainValid.get()) return true;
    else throw new GlobalExceptions.StructuredDataNotAccessibleException();
  }

  /**
   * Function that checks if a CurationLink has a domain the current user has access to, or if the
   * user only has a single domain sets theCurationLink to that domain.
   *
   * <p>May return a different version of the CurationLink, so return needs to be stored in future
   * for that CurationLink.
   */
  public CurationLink handleCurationLinkDomain(CurationLink curationLink)
      throws GlobalExceptions.CurationLinkDomainMissingException {

    // get the domains the current user has access to
    Set<String> usersDomains = getDomains();

    if (curationLink.getDomain() == null || curationLink.getDomain().length() == 0) {
      // if the sample doesn't have a domain, and the user has one domain, then they must be
      // submitting to that domain
      if (usersDomains.size() == 1) {
        curationLink =
            CurationLink.build(
                curationLink.getSample(),
                curationLink.getCuration(),
                usersDomains.iterator().next(),
                null,
                curationLink.getCreated());
      } else {
        // if the sample doesn't have a domain, and we can't guess one, then end
        throw new GlobalExceptions.CurationLinkDomainMissingException();
      }
    }

    // check sample is assigned to a domain that the authenticated user has access to
    if (usersDomains.contains(bioSamplesProperties.getBiosamplesAapSuperWrite())) {
      return curationLink;
    } else if (usersDomains.contains(curationLink.getDomain())) {
      return curationLink;
    } else {
      log.info(
          "User asked to submit curation to domain "
              + curationLink.getDomain()
              + " but has access to "
              + usersDomains);
      throw new GlobalExceptions.SampleNotAccessibleException();
    }
  }

  public boolean isWriteSuperUser() {
    return getDomains().contains(bioSamplesProperties.getBiosamplesAapSuperWrite());
  }

  public boolean isIntegrationTestUser() {
    return getDomains().contains("self.BiosampleIntegrationTest");
  }

  public void checkSampleAccessibility(Sample sample)
      throws GlobalExceptions.SampleNotAccessibleException {
    // TODO throw different exceptions in different situations
    if (sample.getRelease().isBefore(Instant.now())) {
      // release date in past, accessible
    } else if (getDomains().contains(bioSamplesProperties.getBiosamplesAapSuperRead())) {
      // if the current user belongs to a super read domain, accessible
    } else if (getDomains().contains(sample.getDomain())) {
      // if the current user belongs to a domain that owns the sample, accessible
    } else {
      throw new GlobalExceptions.SampleNotAccessibleException();
    }
  }
}
