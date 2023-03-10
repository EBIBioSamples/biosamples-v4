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

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.tsc.aap.client.model.Domain;

@Component
@Primary
public class AapDomainRepositoryRest {
  private final RestTemplate template;

  public AapDomainRepositoryRest(
      @Value("${aap.domains.url}") final String domainsApiUrl,
      @Value("${aap.timeout:180000}") final int timeout,
      final RestTemplateBuilder clientBuilder) {
    template =
        clientBuilder
            .rootUri(domainsApiUrl)
            .setConnectTimeout(Duration.ofSeconds(timeout))
            .setReadTimeout(Duration.ofSeconds(timeout))
            .build();
  }

  /**
   * * Get logged in user membership domains
   *
   * @param token - user token
   * @return list of membership domains
   */
  Collection<Domain> getMyDomains(final String token) {
    final HttpEntity<String> entity = new HttpEntity<>("parameters", createHeaders(token));
    final ResponseEntity<List<Domain>> response =
        template.exchange(
            "/my/domains",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Domain>>() {});
    return response.getBody();
  }

  private HttpHeaders createHeaders(final String token) {
    return new HttpHeaders() {
      {
        final String authHeader = "Bearer " + token;
        set("Authorization", authHeader);
      }
    };
  }
}
