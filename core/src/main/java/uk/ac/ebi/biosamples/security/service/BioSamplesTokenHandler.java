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

import java.util.ArrayList;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.security.model.AuthToken;

@Component
@Slf4j
public class BioSamplesTokenHandler {
  @Autowired AccessControlService accessControlService;

  public User getUser(final String token) {
    try {
      final Optional<AuthToken> authToken = accessControlService.extractToken(token);

      if (authToken.isEmpty()) {
        throw new RuntimeException("No claims for this token");
      } else {
        return new User(authToken.get().getUser(), "", new ArrayList<>());
      }
    } catch (final Exception e) {
      log.info("Cannot parse token: " + e.getMessage());
    }

    return new User(null, "", new ArrayList<>());
  }
}
