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

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AccessControlServiceTest extends TestCase {
  private static final String EXPIRED_WEBIN_TOKEN =
      "eyJhbGciOiJSUzI1NiJ9.eyJwcmluY2lwbGUiOiJXZWJpbi01OTI4NyIsInJvbGUiO"
          + "ltdLCJleHAiOjE2MjY4ODE5MjgsImlhdCI6MTYyNjg2MzkyOH0.OkgsxRLGkG0O5nbVnVsgwKRNMM3Fqh4bsNRqM0n0fTWLqqqBc"
          + "J4tNgaihj7OmZmCpIKTOecxEhh3anNfjQQ1O9vQhtCeiFz9g2Tj8pTdv-6FBZ5t5gidz5W4GDsJ_8hDnXPge7Gk5ug3_GddDAWHv"
          + "wJhuK_OR5oIIAf6SBeWNr9HKLpOQYcywYsrmKAFjTgA-wrGWtcR3qvFVDiQCpW2UzB8kzFVKdegIdrI2PgQnP5e0f5BoQ5V-qo7W"
          + "Bwn81bW7NkWHBXVecMab_UsKUyTMqNbsFY5TGJNj715a1Z_N6npkynGCpB3VbR5X6L3JVEnlhkBoCTE9zKUbfa3KLglYA";
  private final AccessControlService accessControlService =
      new AccessControlService(new ObjectMapper());

  public void testExtractToken() {
    try {
      accessControlService.extractToken(EXPIRED_WEBIN_TOKEN).orElse(null);
    } catch (final Exception e) {
      assertTrue(e instanceof ResponseStatusException);

      final ResponseStatusException rse = (ResponseStatusException) e;

      assertSame(rse.getStatus(), HttpStatus.UNAUTHORIZED);
    }
  }
}
