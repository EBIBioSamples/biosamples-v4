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

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleServiceV2;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.TokenService;
import uk.ac.ebi.tsc.aap.client.security.UserAuthentication;

@Service
public class BioSamplesAapServiceV2 {

  private Logger log = LoggerFactory.getLogger(getClass());

  private final Traverson traverson;
  private final BioSamplesProperties bioSamplesProperties;
  private final SampleServiceV2 sampleServiceV2;
  private final BioSamplesWebinAuthenticationServiceV2 bioSamplesWebinAuthenticationServiceV2;

  public BioSamplesAapServiceV2(
      RestTemplateBuilder restTemplateBuilder,
      BioSamplesProperties bioSamplesProperties,
      SampleServiceV2 sampleServiceV2,
      BioSamplesWebinAuthenticationServiceV2 bioSamplesWebinAuthenticationServiceV2) {
    traverson =
        new Traverson(bioSamplesProperties.getBiosamplesClientAapUri(), MediaTypes.HAL_JSON);
    traverson.setRestOperations(restTemplateBuilder.build());
    this.bioSamplesProperties = bioSamplesProperties;
    this.sampleServiceV2 = sampleServiceV2;
    this.bioSamplesWebinAuthenticationServiceV2 = bioSamplesWebinAuthenticationServiceV2;
  }

  @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample must specify a domain") // 400
  public static class DomainMissingException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.FORBIDDEN,
      reason =
          "This sample is private and not available for browsing. If you think this is an error and/or you should have access please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk") // 403
  public static class SampleNotAccessibleException extends RuntimeException {}

  @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample domain mismatch") // 400
  public static class SampleDomainMismatchException extends RuntimeException {}

  /**
   * Returns a set of domains that the current user has access to (uses thread-bound spring
   * security) Always returns a set, even if its empty if not logged in
   *
   * @return
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
   *
   * @param sample
   * @return
   * @throws SampleNotAccessibleException
   * @throws DomainMissingException
   */
  public Sample handleSampleDomain(Sample sample)
      throws SampleNotAccessibleException, DomainMissingException {
    // get the domains the current user has access to
    Set<String> usersDomains = getDomains();

    if (sample.getDomain() == null || sample.getDomain().length() == 0) {
      // if the sample doesn't have a domain, and the user has one domain, then they must be
      // submitting to that domain
      if (usersDomains.size() == 1) {
        sample =
            Sample.Builder.fromSample(sample)
                .withDomain(usersDomains.iterator().next())
                .withNoWebinSubmissionAccountId()
                .build();
      } else {
        throw new DomainMissingException();
      }
    }

    if (sample.getAccession() != null && !(isWriteSuperUser() || isIntegrationTestUser())) {
      Optional<Sample> oldSample =
          sampleServiceV2.fetch(sample.getAccession(), Optional.empty(), null);
      final boolean oldSamplePresent = oldSample.isPresent();

      if (!oldSamplePresent || !usersDomains.contains(oldSample.get().getDomain())) {
        final boolean webinProxyUser =
            (oldSamplePresent
                && bioSamplesWebinAuthenticationServiceV2.isWebinSuperUser(
                    oldSample.get().getWebinSubmissionAccountId()));

        log.info("WEBIN proxy user " + webinProxyUser);

        if (!webinProxyUser) {
          throw new SampleDomainMismatchException();
        }
      }
    }

    // check sample is assigned to a domain that the authenticated user has access to
    if (usersDomains.contains(bioSamplesProperties.getBiosamplesAapSuperWrite())) {
      return sample;
    } else if (usersDomains.contains(sample.getDomain())) {
      return sample;
    } else {
      log.warn(
          "User asked to submit sample to domain "
              + sample.getDomain()
              + " but has access to "
              + usersDomains);
      throw new SampleNotAccessibleException();
    }
  }

  public boolean isWriteSuperUser() {
    return getDomains().contains(bioSamplesProperties.getBiosamplesAapSuperWrite());
  }

  public boolean isIntegrationTestUser() {
    return getDomains().contains("self.BiosampleIntegrationTest");
  }
}
