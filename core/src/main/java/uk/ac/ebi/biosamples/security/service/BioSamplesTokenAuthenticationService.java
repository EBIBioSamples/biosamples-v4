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
package uk.ac.ebi.biosamples.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.security.model.UserAuthentication;

@Service
@Primary
@Slf4j
public class BioSamplesTokenAuthenticationService {
  private final BioSamplesTokenHandler tokenHandler;

  public BioSamplesTokenAuthenticationService(final BioSamplesTokenHandler tokenHandler) {
    super();
    this.tokenHandler = tokenHandler;
  }

  public Authentication getAuthenticationFromToken(String token) {
    log.trace("getAuthentication");

    try {
      if (token == null) {
        return null;
      }

      return new UserAuthentication(tokenHandler.getUser(token));
    } catch (final Exception e) {
      log.error(e.getMessage());
      log.debug("Cannot extract authentication details from token", e);

      return null;
    }
  }
}
