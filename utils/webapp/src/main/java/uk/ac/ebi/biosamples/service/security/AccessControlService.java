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

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;

@Service
public class AccessControlService {
  private final ObjectMapper objectMapper;

  public AccessControlService(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Optional<AuthToken> extractToken(String token) {
    try {
      if (token == null || token.isEmpty()) {
        return Optional.empty();
      }

      token = token.startsWith("Bearer ") ? token.split("Bearer ")[1] : token;

      final String[] chunks = token.split("\\.");
      final Base64.Decoder decoder = Base64.getDecoder();
      final String header = new String(decoder.decode(chunks[0]));
      final String payload = new String(decoder.decode(chunks[1]));

      if (!verifySignature()) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
      }

      final AuthToken authToken;

      try {
        final String algorithm = objectMapper.readTree(header).get("alg").asText();
        final AuthorizationProvider authority;
        final String user;
        final List<String> roles;

        final JsonNode node = objectMapper.readTree(payload);
        if (isAap(node)) {
          authority = AuthorizationProvider.AAP;
          user = node.get("sub").asText();
          roles =
              objectMapper.convertValue(node.get("domains"), new TypeReference<List<String>>() {});
        } else {
          verifyValidity(token);

          authority = AuthorizationProvider.WEBIN;
          user = node.get("principle").asText();
          roles = objectMapper.convertValue(node.get("role"), new TypeReference<>() {});
        }

        authToken = new AuthToken(algorithm, authority, user, roles);
      } catch (final IOException e) {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Invalid authentication details provided", e);
      }

      return Optional.of(authToken);
    } catch (final Exception e) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Invalid authentication details provided", e);
    }
  }

  private void verifyValidity(final String token) {
    final DecodedJWT jwt = JWT.decode(token);

    if (jwt.getExpiresAt().before(new Date())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  private static boolean isAap(final JsonNode node) {
    return node.get("iss") != null && node.get("iss").asText().contains("aai.ebi.ac.uk");
  }

  private boolean verifySignature() {
    return true;
  }

  public List<String> getUserRoles(final AuthToken token) {
    return token.getAuthority() == AuthorizationProvider.AAP
        ? token.getRoles()
        : Collections.singletonList(token.getUser());
  }
}
