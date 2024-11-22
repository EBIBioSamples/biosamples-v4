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

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

@Slf4j
public class BioSamplesTokenAuthenticationFilter extends GenericFilterBean {
  private final BioSamplesTokenAuthenticationService bioSamplesTokenAuthenticationService;

  public BioSamplesTokenAuthenticationFilter(
      final BioSamplesTokenAuthenticationService bioSamplesTokenAuthenticationService) {
    this.bioSamplesTokenAuthenticationService = bioSamplesTokenAuthenticationService;
  }

  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
      throws IOException, ServletException {
    log.info("Token filter invoked.");

    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final String headerToken = getHeaderToken(httpRequest);

    if (headerToken != null) {
      // JWT auth.
      final Authentication authentication =
          bioSamplesTokenAuthenticationService.getAuthenticationFromToken(headerToken);

      if (authentication != null) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }

  private static String getHeaderToken(final HttpServletRequest request) {
    try {
      final String header = request.getHeader("Authorization");

      if (header == null) {
        log.info("No Authorization header");

        return null;
      }

      if (!header.trim().startsWith("Bearer".trim())) {
        log.info("No {} prefix", "Bearer");

        return null;
      } else {
        final String token = header.substring("Bearer".trim().length());

        if (token.isEmpty()) {
          log.info("Missing jwt token");

          return null;
        } else {
          return token.trim();
        }
      }
    } catch (final Exception e) {
      return null;
    }
  }
}
