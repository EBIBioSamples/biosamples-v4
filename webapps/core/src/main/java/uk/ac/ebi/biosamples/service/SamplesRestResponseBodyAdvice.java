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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
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
import uk.ac.ebi.biosamples.core.model.Sample;

@RestControllerAdvice(assignableTypes = SamplesRestController.class)
public class SamplesRestResponseBodyAdvice
    implements ResponseBodyAdvice<PagedModel<EntityModel<Sample>>> {

  @Override
  public boolean supports(
      final MethodParameter returnType,
      final Class<? extends HttpMessageConverter<?>> converterType) {
    return Objects.requireNonNull(returnType.getMethod()).getReturnType() == PagedModel.class;
  }

  @Override
  public PagedModel<EntityModel<Sample>> beforeBodyWrite(
      final PagedModel<EntityModel<Sample>> body,
      final MethodParameter returnType,
      final MediaType selectedContentType,
      final Class<? extends HttpMessageConverter<?>> selectedConverterType,
      final ServerHttpRequest request,
      final ServerHttpResponse response) {

    // TODO application.properties the cache time

    if (Objects.equals(request.getMethod(), HttpMethod.GET)) {
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
