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
package uk.ac.ebi.biosamples.client.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.client.Traverson;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import uk.ac.ebi.biosamples.client.utils.IterableResourceFetchAll;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;

public class SampleCursorRetrievalService {

  private static final ParameterizedTypeReference<PagedModel<EntityModel<Sample>>>
      parameterizedTypeReferencePagedResourcesSample =
          new ParameterizedTypeReference<PagedModel<EntityModel<Sample>>>() {};

  private final Traverson traverson;
  private final ExecutorService executor;
  private final RestOperations restOperations;
  private final int pageSize;

  public SampleCursorRetrievalService(
      RestOperations restOperations, Traverson traverson, ExecutorService executor, int pageSize) {
    this.restOperations = restOperations;
    this.traverson = traverson;
    this.executor = executor;
    this.pageSize = pageSize;
  }

  public Iterable<EntityModel<Sample>> fetchAll(String text, Collection<Filter> filterCollection) {
    return fetchAll(text, filterCollection, null);
  }

  public Iterable<EntityModel<Sample>> fetchAll(
      String text, Collection<Filter> filterCollection, String jwt) {
    return fetchAll(text, filterCollection, jwt, true);
  }

  public Iterable<EntityModel<Sample>> fetchAll(
      String text, Collection<Filter> filterCollection, String jwt, boolean addCurations) {

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("text", text);
    for (Filter filter : filterCollection) {
      params.add("filter", filter.getSerialization());
    }
    params.add("size", Integer.toString(pageSize));
    if (!addCurations) {
      params.add("curationdomain", "");
    }

    params = encodePlusInQueryParameters(params);

    return new IterableResourceFetchAll<>(
        executor,
        traverson,
        restOperations,
        parameterizedTypeReferencePagedResourcesSample,
        jwt,
        params,
        "samples",
        "cursor");
  }

  // TODO to keep the + in a (not encoded) query parameter is to force encoding
  private MultiValueMap<String, String> encodePlusInQueryParameters(
      MultiValueMap<String, String> queryParameters) {
    MultiValueMap<String, String> encodedQueryParameters = new LinkedMultiValueMap<>();
    for (Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
      String key = param.getKey();
      param
          .getValue()
          .forEach(
              v -> {
                if (v != null) {
                  encodedQueryParameters.add(key, v.replaceAll("\\+", "%2B"));
                } else {
                  encodedQueryParameters.add(key, "");
                }
              });
    }
    return encodedQueryParameters;
  }
}
