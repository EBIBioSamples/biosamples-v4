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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.exception.AccessControlException;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;

@Service
public class AccessControlService {
  private Logger log = LoggerFactory.getLogger(getClass());
  private final ObjectMapper objectMapper;

  public AccessControlService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Optional<AuthToken> extractToken(String token) {
    if (token == null || token.isEmpty()) {
      return Optional.empty();
    }

    token = token.startsWith("Bearer ") ? token.split("Bearer ")[1] : token;

    String[] chunks = token.split("\\.");
    Base64.Decoder decoder = Base64.getDecoder();
    String header = new String(decoder.decode(chunks[0]));
    String payload = new String(decoder.decode(chunks[1]));

    if (!verifySignature(token)) {
      throw new AccessControlException("Failed to verify the integrity of the token");
    }

    AuthToken authToken;
    try {
      String algorithm = objectMapper.readTree(header).get("alg").asText();
      AuthorizationProvider authority;
      String user;
      List<String> roles;

      JsonNode node = objectMapper.readTree(payload);
      if (isAap(node)) {
        authority = AuthorizationProvider.AAP;
        user = node.get("sub").asText();
        roles =
            objectMapper.convertValue(node.get("domains"), new TypeReference<List<String>>() {});
      } else {
        authority = AuthorizationProvider.WEBIN;
        user = node.get("principle").asText();
        roles = objectMapper.convertValue(node.get("role"), new TypeReference<List<String>>() {});
      }

      authToken = new AuthToken(algorithm, authority, user, roles);
    } catch (IOException e) {
      throw new AccessControlException("Could not decode token", e);
    }

    return Optional.of(authToken);
  }

  private static boolean isAap(JsonNode node) {
    return node.get("iss") != null && node.get("iss").asText().contains("aai.ebi.ac.uk");
  }

  public boolean verifySignature(String token) {
    // todo without verifying with the authority we cant make any claims about the validity of the
    // token
    return true;
  }

  public List<String> getUserRoles(AuthToken token) {
    return token.getAuthority() == AuthorizationProvider.AAP
        ? token.getRoles()
        : Collections.singletonList(token.getUser());
  }
}
