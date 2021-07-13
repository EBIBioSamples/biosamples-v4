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
package uk.ac.ebi.biosamples.service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;
import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;
import uk.ac.ebi.biosamples.mongo.repo.MongoSubmissionRepository;

/**
 * Based on https://xsolve.software/blog/redis-with-spring-mvc/
 *
 * @author faulcon
 */
@Component
public class SubmissionArchiveFilter extends OncePerRequestFilter {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Autowired private MongoSubmissionRepository mongoSubmissionRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // pre-read all of a put or post request
    if (HttpMethod.POST.matches(request.getMethod())
        || HttpMethod.PUT.matches(request.getMethod())
        || HttpMethod.PATCH.matches(request.getMethod())
        || HttpMethod.DELETE.matches(request.getMethod())) {

      // store up to 32MB of cache
      // big enough to deal with all legit subs
      // small enough not to be broken by malicious ones
      ContentCachingRequestWrapper requestWrapper =
          new ContentCachingRequestWrapper(request, 32 * 1024);
      try {
        filterChain.doFilter(requestWrapper, response);
      } finally {
        // have to do it in a finally block *after* everything has
        // already been received

        // read the request to a string
        final ContentCachingRequestWrapper wrapperAfterRequest =
            WebUtils.getNativeRequest(requestWrapper, ContentCachingRequestWrapper.class);
        final String content =
            new String(
                wrapperAfterRequest.getContentAsByteArray(),
                wrapperAfterRequest.getCharacterEncoding());
        log.trace("content = " + content);
        // now save the request to the database

        StringBuffer requestStringBuffer = requestWrapper.getRequestURL();
        // this doesn't include query parameters (e.g. ?x=bar) so add them
        if (requestWrapper.getQueryString() != null) {
          requestStringBuffer.append("?");
          requestStringBuffer.append(requestWrapper.getQueryString());
        }
        String url = requestStringBuffer.toString();
        log.trace("url = " + url);

        // store the headers on the request too
        Map<String, List<String>> headers = new HashMap<>();
        Enumeration<String> headerNames = requestWrapper.getHeaderNames();
        while (headerNames.hasMoreElements()) {
          String headerName = headerNames.nextElement();
          if (!headers.containsKey(headerName)) {
            headers.put(headerName, new ArrayList<>());
          }
          String value = requestWrapper.getHeader(headerName);
          headers.get(headerName).add(value);
        }

        // actually do the saving
        log.trace("Saving submission");
        mongoSubmissionRepository.save(
            MongoSubmission.build(Instant.now(), request.getMethod(), url, headers, content));
      }

    } else {
      // continue to the next filter for all other requests
      filterChain.doFilter(request, response);
    }
  }
}
