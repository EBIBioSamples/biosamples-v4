/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import uk.ac.ebi.biosamples.controller.SamplesRestController;
import uk.ac.ebi.biosamples.model.Sample;

@RestControllerAdvice(assignableTypes = SamplesRestController.class)
public class SamplesRestResponseBodyAdvice
    implements ResponseBodyAdvice<PagedResources<Resource<Sample>>> {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return returnType.getMethod().getReturnType() == PagedResources.class;
  }

  @Override
  public PagedResources<Resource<Sample>> beforeBodyWrite(
      PagedResources<Resource<Sample>> body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {

    // TODO application.properties the cache time

    if (request.getMethod().equals(HttpMethod.GET)) {
      // if there is an authorization header, then keep it private
      if (request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
        response
            .getHeaders()
            .setCacheControl(
                CacheControl.maxAge(1, TimeUnit.MINUTES).cachePrivate().getHeaderValue());
        response.getHeaders().setPragma("no-cache"); // for HTTP/1.0 backwards compatibility
      } else {
        // no authorization, public request
        response
            .getHeaders()
            .setCacheControl(
                CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic().getHeaderValue());
      }
    }

    return body;
  }
}
