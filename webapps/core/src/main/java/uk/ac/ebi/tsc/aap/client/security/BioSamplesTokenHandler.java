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
package uk.ac.ebi.tsc.aap.client.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;

@Component
public class BioSamplesTokenHandler extends TokenHandler {
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public User parseUserFromToken(final String token) {
    try {
      final Set<Domain> domainsSet = new HashSet<>();
      final JwtClaims jwtClaims = jwtConsumer.processToClaims(token);
      final String userReference = jwtClaims.getSubject();
      final String nickname = jwtClaims.getStringClaimValue("nickname");
      final String email = jwtClaims.getStringClaimValue("email");
      final String fullName = jwtClaims.getStringClaimValue("name");
      final List<String> domains = jwtClaims.getStringListClaimValue("domains");

      domains.forEach(name -> domainsSet.add(new Domain(name, null, null)));

      return new User(nickname, email, userReference, fullName, domainsSet);
    } catch (final Exception e) {
      return tryParsingWebinAuthToken(token);
    }
  }

  private User tryParsingWebinAuthToken(final String token) {
    try {
      final Claims claims = decodeJWT(token);

      if (claims == null) {
        throw new RuntimeException("No claims for this token");
      } else {
        return new User(null, null, claims.get("principle", String.class), null, null);
      }
    } catch (final Exception e) {
      log.info("Cannot parse token: " + e.getMessage());
    }

    return new User(null, null, null, null, null);
  }

  public Claims decodeJWT(String jwt) {
    final int i = jwt.lastIndexOf('.');
    final String withoutSignature = jwt.substring(0, i + 1);

    return Jwts.parser().parseClaimsJwt(withoutSignature).getBody();
  }
}
