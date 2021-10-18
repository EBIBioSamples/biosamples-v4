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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import uk.ac.ebi.biosamples.model.CurationLink;

import java.net.URI;
import java.util.concurrent.ExecutorService;

public class CurationSubmissionService {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Traverson traverson;
  private final ExecutorService executor;
  private final RestOperations restOperations;

  public CurationSubmissionService(
      RestOperations restOperations, Traverson traverson, ExecutorService executor) {
    this.restOperations = restOperations;
    this.traverson = traverson;
    this.executor = executor;
  }

  public EntityModel<CurationLink> submit(CurationLink curationLink, boolean isWebin)
      throws RestClientException {
    return persistCuration(curationLink, null, isWebin);
  }

  public EntityModel<CurationLink> persistCuration(
      CurationLink curationLink, String jwt, boolean isWebin) throws RestClientException {
    String addWebinRequestParam = "";

    if (isWebin) {
      addWebinRequestParam = "?authProvider=WEBIN";
    }

    URI target =
        URI.create(
            traverson
                .follow("samples")
                .follow(Hop.rel("sample").withParameter("accession", curationLink.getSample()))
                .follow("curationLinks")
                .asLink()
                .getHref()
                .concat(addWebinRequestParam));

    log.trace("POSTing to " + target + " " + curationLink);

    RequestEntity.BodyBuilder bodyBuilder =
        RequestEntity.post(target)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaTypes.HAL_JSON);
    if (jwt != null) {
      bodyBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
    }
    RequestEntity<CurationLink> requestEntity = bodyBuilder.body(curationLink);

    ResponseEntity<EntityModel<CurationLink>> responseEntity =
        restOperations.exchange(
            requestEntity, new ParameterizedTypeReference<EntityModel<CurationLink>>() {});

    return responseEntity.getBody();
  }

  public void deleteCurationLink(String sample, String hash) {
    deleteCurationLink(sample, hash, null);
  }

  public void deleteCurationLink(String sample, String hash, String jwt) {

    URI target =
        URI.create(
            traverson
                .follow("samples")
                .follow(Hop.rel("sample").withParameter("accession", sample))
                .follow(Hop.rel("curationLink").withParameter("hash", hash))
                .asLink()
                .getHref());
    log.trace("DELETEing " + target);

    RequestEntity requestEntity;
    if (jwt != null) {
      requestEntity =
          RequestEntity.delete(target).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt).build();
    } else {
      requestEntity = RequestEntity.delete(target).build();
    }

    restOperations.exchange(requestEntity, Void.class);
  }
}
