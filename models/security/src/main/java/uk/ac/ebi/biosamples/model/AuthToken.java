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
package uk.ac.ebi.biosamples.model;

import java.util.List;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;

public class AuthToken {
  private final String algorithm;
  private final AuthorizationProvider authority;
  private final String user;
  private final List<String> roles;

  public AuthToken(
      final String algorithm,
      final AuthorizationProvider authority,
      final String user,
      final List<String> roles) {
    this.algorithm = algorithm;
    this.authority = authority;
    this.user = user;
    this.roles = roles;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public AuthorizationProvider getAuthority() {
    return authority;
  }

  public String getUser() {
    return user;
  }

  public List<String> getRoles() {
    return roles;
  }
}
