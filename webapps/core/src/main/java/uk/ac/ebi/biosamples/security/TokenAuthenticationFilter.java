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
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

public class TokenAuthenticationFilter extends GenericFilterBean {
  private static final String TOKEN_HEADER_KEY = "Authorization";
  private static final String TOKEN_HEADER_VALUE_PREFIX = "Bearer";
  private final BioSamplesTokenAuthenticationService authenticationService;

  public TokenAuthenticationFilter(
      final BioSamplesTokenAuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
      throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final String headerToken = getHeaderToken(httpRequest);

    if (headerToken != null) {
      // JWT auth.
      final Authentication authentication =
          authenticationService.getAuthenticationFromToken(headerToken);

      if (authentication != null) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }

  private static String getHeaderToken(final HttpServletRequest request) {
    final String header = request.getHeader(TOKEN_HEADER_KEY);

    if (header == null) {
      return null;
    } else if (!header.trim().startsWith(TOKEN_HEADER_VALUE_PREFIX.trim())) {
      return null;
    }

    final String token = header.substring(TOKEN_HEADER_VALUE_PREFIX.trim().length());

    if (StringUtils.isEmpty(token)) {
      return null;
    }

    return token;
  }
}
