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
package uk.ac.ebi.tsc.aap.client.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import uk.ac.ebi.tsc.aap.client.model.User;

@Service
@Primary
public class BioSamplesTokenAuthenticationService extends TokenAuthenticationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BioSamplesTokenAuthenticationService.class);
  private final BioSamplesTokenHandler tokenHandler;

  public BioSamplesTokenAuthenticationService(final BioSamplesTokenHandler tokenHandler) {
    super(tokenHandler);
    this.tokenHandler = tokenHandler;
  }

  public Authentication getAuthenticationFromToken(String token){
    LOGGER.trace("getAuthentication");
    try {
      if (token == null) return null;
      User user = tokenHandler.parseUserFromToken(token);
      return new UserAuthentication(user);
    }
    catch(Exception e) {
      LOGGER.error(e.getMessage());
      LOGGER.debug("Cannot extract authentication details from token", e);
      return null;
    }
  }
}
