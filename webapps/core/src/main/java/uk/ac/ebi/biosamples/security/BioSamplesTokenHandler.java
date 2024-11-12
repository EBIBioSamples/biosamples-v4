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
package uk.ac.ebi.biosamples.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class BioSamplesTokenHandler {
  private final Logger log = LoggerFactory.getLogger(getClass());

  public User getUser(final String token) {
    Claims claims = null;

    try {
      claims = decodeJWT(token);

      if (claims == null) {
        throw new RuntimeException("No claims for this token");
      } else {
        return new User(null, null, null);
      }
    } catch (final Exception e) {
      log.info("Cannot parse token: " + e.getMessage());
    }

    return new User(claims.get("principle", String.class), null, null);
  }

  private Claims decodeJWT(final String jwt) {
    final int i = jwt.lastIndexOf('.');
    final String withoutSignature = jwt.substring(0, i + 1);

    return Jwts.parser().parseClaimsJwt(withoutSignature).getBody();
  }
}
