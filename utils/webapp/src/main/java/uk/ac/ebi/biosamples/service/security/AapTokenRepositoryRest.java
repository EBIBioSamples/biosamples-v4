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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import uk.ac.ebi.tsc.aap.client.exception.UserNameOrPasswordWrongException;
import uk.ac.ebi.tsc.aap.client.repo.TokenRepository;

@Component
@Primary
public class AapTokenRepositoryRest implements TokenRepository {
  private final String domainsApiUrl;
  private final RestTemplate template;

  public AapTokenRepositoryRest(
      @Value("${aap.domains.url}") final String domainsApiUrl,
      @Value("${aap.timeout:180000}") final int timeout,
      final RestTemplateBuilder clientBuilder) {
    template =
        clientBuilder
            .rootUri(domainsApiUrl)
            .setConnectTimeout(Duration.ofSeconds(timeout))
            .setReadTimeout(Duration.ofSeconds(timeout))
            .build();
    this.domainsApiUrl = domainsApiUrl;
  }

  @Override
  public String getAAPToken(final String username, final String password)
      throws UserNameOrPasswordWrongException {
    final ResponseEntity response;

    try {
      final HttpEntity<String> entity = new HttpEntity(createHttpHeaders(username, password));
      response =
          template.exchange(
              domainsApiUrl + "/auth", HttpMethod.GET, entity, String.class, new Object[0]);
    } catch (final HttpClientErrorException var5) {
      throw new UserNameOrPasswordWrongException(
          String.format("username/password wrong. Please check username or password to get token"),
          var5);
    } catch (final Exception var6) {
      throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return (String) response.getBody();
  }

  private static HttpHeaders createHttpHeaders(final String username, final String password) {
    final HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Basic " + Base64Coder.encodeString(username + ":" + password));
    return headers;
  }
}
