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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import uk.ac.ebi.biosamples.client.utils.IterableResourceFetchAll;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;

public class CurationRetrievalService {

  private final Traverson traverson;
  private final ExecutorService executor;
  private final RestOperations restOperations;
  private final int pageSize;

  public CurationRetrievalService(
      final RestOperations restOperations, final Traverson traverson, final int pageSize) {
    this.restOperations = restOperations;
    this.traverson = traverson;
    this.executor = Executors.newSingleThreadExecutor();
    this.pageSize = pageSize;
  }

  public Iterable<EntityModel<Curation>> fetchAll() {
    return fetchAll(null);
  }

  public Iterable<EntityModel<Curation>> fetchAll(final String jwt) {
    final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("size", Integer.toString(pageSize));
    return new IterableResourceFetchAll<>(
        executor,
        traverson,
        restOperations,
        new ParameterizedTypeReference<PagedModel<EntityModel<Curation>>>() {},
        jwt,
        params,
        "curations");
  }

  public Iterable<EntityModel<CurationLink>> fetchCurationLinksOfSample(final String accession) {
    return fetchCurationLinksOfSample(accession, null);
  }

  public Iterable<EntityModel<CurationLink>> fetchCurationLinksOfSample(
      final String accession, final String jwt) {
    final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("size", Integer.toString(pageSize));
    return new IterableResourceFetchAll<>(
        executor,
        traverson,
        restOperations,
        new ParameterizedTypeReference<PagedModel<EntityModel<CurationLink>>>() {},
        jwt,
        params,
        Hop.rel("samples"),
        Hop.rel("sample").withParameter("accession", accession),
        Hop.rel("curationLinks"));
  }
}
