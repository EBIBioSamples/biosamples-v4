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
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.structured.StructuredData;

import java.net.URI;
import java.util.concurrent.ExecutorService;

public class StructuredDataSubmissionService {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Traverson traverson;
  private final ExecutorService executor;
  private final RestOperations restOperations;

  public StructuredDataSubmissionService(
      RestOperations restOperations, Traverson traverson, ExecutorService executor) {
    this.restOperations = restOperations;
    this.traverson = traverson;
    this.executor = executor;
  }

  public Resource<StructuredData> submit(StructuredData structuredData, boolean isWebin) throws RestClientException {
    return persistStructuredData(structuredData, null, isWebin);
  }

  public Resource<StructuredData> persistStructuredData(
      StructuredData structuredData, String jwt, boolean isWebin) throws RestClientException {
    String addWebinRequestParam = "";

    if (isWebin) {
      addWebinRequestParam = "?authProvider=WEBIN";
    }

    URI target =
        URI.create(
            traverson
                .follow("samples")
                .follow(Hop.rel("sample").withParameter("accession", structuredData.getAccession()))
                .follow("structuredData")
                .asLink()
                .getHref()
                .concat(addWebinRequestParam));

    log.info("PUTing to " + target + " " + structuredData);

    RequestEntity.BodyBuilder bodyBuilder =
        RequestEntity.put(target)
            .contentType(MediaType.APPLICATION_JSON);
    if (jwt != null) {
      bodyBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
    }
    RequestEntity<StructuredData> requestEntity = bodyBuilder.body(structuredData);

    ResponseEntity<Resource<StructuredData>> responseEntity =
        restOperations.exchange(
            requestEntity, new ParameterizedTypeReference<Resource<StructuredData>>() {});

    return responseEntity.getBody();
  }
}
