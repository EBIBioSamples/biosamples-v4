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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.exception.AccessControlException;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.auth.LoginWays;

@Service
public class AccessControlService {
  private Logger log = LoggerFactory.getLogger(getClass());
  private final ObjectMapper objectMapper;

  public AccessControlService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public AuthToken extractToken(String token) {
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
      LoginWays authority;
      String user;
      List<String> roles;

      JsonNode node = objectMapper.readTree(payload);
      if (node.get("iss") != null && isIss(node)) {
        authority = LoginWays.AAP;
        user = node.get("sub").asText();
        roles =
            objectMapper.convertValue(node.get("domains"), new TypeReference<List<String>>() {});
      } else {
        authority = LoginWays.WEBIN;
        user = node.get("principle").asText();
        roles = objectMapper.convertValue(node.get("role"), new TypeReference<List<String>>() {});
      }

      authToken = new AuthToken(algorithm, authority, user, roles);
    } catch (IOException e) {
      throw new AccessControlException("Could not decode token. ", e);
    }

    return authToken;
  }

  private boolean isIss(JsonNode node) {
    final String iss = node.get("iss").asText();
    log.info("ISS is " + iss);

    return "https://explore.aai.ebi.ac.uk/sp".equals(iss) || "https://aai.ebi.ac.uk/sp".equals(iss);
  }

  public boolean verifySignature(String token) {
    // todo without verifying with the authority we cant make any claims about the validity of the
    // token
    return true;
  }

  public List<String> getUserRoles(AuthToken token) {
    return token.getAuthority() == LoginWays.AAP
        ? token.getRoles()
        : Collections.singletonList(token.getUser());
  }
}
