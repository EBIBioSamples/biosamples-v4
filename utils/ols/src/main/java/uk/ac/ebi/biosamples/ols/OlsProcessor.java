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
package uk.ac.ebi.biosamples.ols;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.utils.ClientUtils;

@Service
public class OlsProcessor {

  private Logger log = LoggerFactory.getLogger(getClass());

  private final RestTemplate restTemplate;

  private final BioSamplesProperties bioSamplesProperties;

  public OlsProcessor(RestTemplate restTemplate, BioSamplesProperties bioSamplesProperties) {
    this.restTemplate = restTemplate;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  /**
   * @param ontology must be unencoded
   * @param iri must be unencoded
   * @return
   */
  @Cacheable("ols_ancestors_synonyms")
  public Collection<String> ancestorsAndSynonyms(String ontology, String iri) {
    Set<String> synonyms = new HashSet<>();
    if (ontology == null || ontology.trim().length() == 0) {
      return synonyms;
    }
    if (iri == null || iri.trim().length() == 0) {
      return synonyms;
    }

    // check if the iri is a full iri with all the necessary parts
    // build has to flag this iri as having already been encoded
    UriComponents iriComponents = UriComponentsBuilder.fromUriString(iri).build();
    if (iriComponents.getScheme() == null
        || iriComponents.getHost() == null
        || iriComponents.getPath() == null) {
      // incomplete iri (e.g. 9606, EFO_12345) don't bother to check
      return synonyms;
    }

    // TODO do more by hal links, needs OLS to support
    // build has to flag this iri as having already been encoded
    UriComponents uriComponents =
        UriComponentsBuilder.fromUriString(
                bioSamplesProperties.getOls()
                    + "/api/ontologies/{ontology}/terms/{term}/hierarchicalAncestors?size=1000")
            .build();

    log.trace("Base uriComponents = " + uriComponents);

    // have to *double* encode the things we are going to put in the URI
    // uriComponents will encode it once, so we only need to encode it one more time manually
    ontology = UriUtils.encodePathSegment(ontology, "UTF-8");
    iri = UriUtils.encodePathSegment(iri, "UTF-8");
    // expand the template using the variables
    URI uri = uriComponents.expand(ontology, iri).toUri();

    log.debug("Contacting " + uri);

    // Note: OLS won't accept hal+json on that endpoint
    RequestEntity<Void> requestEntity =
        RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
    ResponseEntity<JsonNode> responseEntity = null;
    try {
      responseEntity =
          ClientUtils.doRetryQuery(
              requestEntity, restTemplate, 5, new ParameterizedTypeReference<JsonNode>() {});
    } catch (HttpStatusCodeException e) {
      // if we get a 404, return an empty list
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Collections.emptyList();
      }
    }

    JsonNode n = responseEntity.getBody();
    if (n.has("_embedded")) {
      if (n.get("_embedded").has("terms")) {
        for (JsonNode o : n.get("_embedded").get("terms")) {
          if (o.has("label")) {
            String synonym = o.get("label").asText();
            if (synonym != null && synonym.trim().length() > 0) {
              log.trace("adding synonym " + synonym);
              synonyms.add(synonym);
            }
          }
          if (o.has("synonyms")) {
            for (JsonNode p : o.get("synonyms")) {
              String synonym = p.asText();
              if (synonym != null && synonym.trim().length() > 0) {
                log.trace("adding synonym " + synonym);
                synonyms.add(synonym);
              }
            }
          }
        }
      }
    }

    return synonyms;
  }

  // @Cacheable("ols_short")
  public Optional<String> queryOlsForShortcode(String shortcode) {
    log.trace("OLS getting : " + shortcode);

    // TODO do more by hal links, needs OLS to support
    UriComponents uriComponents =
        UriComponentsBuilder.fromUriString(
                bioSamplesProperties.getOls() + "/api/terms?id={shortcode}&size=500")
            .build();
    URI uri = uriComponents.expand(shortcode).encode().toUri();

    log.trace("OLS query for shortcode " + shortcode + " against " + uri);

    RequestEntity<Void> requestEntity =
        RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
    ResponseEntity<ObjectNode> responseEntity =
        restTemplate.exchange(requestEntity, new ParameterizedTypeReference<ObjectNode>() {});

    // non-200 status code
    if (!responseEntity.getStatusCode().is2xxSuccessful()) {
      log.trace(
          "Got status "
              + responseEntity.getStatusCodeValue()
              + " for shortcode "
              + shortcode
              + " against "
              + uri);
      return Optional.empty();
    }

    // if zero result found, abort
    if (responseEntity.getBody() == null) {
      log.trace("Found empty body for shortcode " + shortcode + " against " + uri);
      return Optional.empty();
    }
    ObjectNode n = responseEntity.getBody();

    String iri = null;
    if (n.has("_embedded")) {
      if (n.get("_embedded").has("terms")) {
        for (JsonNode term : n.get("_embedded").get("terms")) {
          if (term.has("iri") && term.has("is_defining_ontology")) {
            log.trace(
                "iri: "
                    + term.get("iri")
                    + ", is_defining_ontology: "
                    + term.get("is_defining_ontology"));
            // if we don't have an iri use this but if there is a defining ontoloy use
            // this in
            // preference
            if (iri == null || term.get("is_defining_ontology").booleanValue()) {
              iri = term.get("iri").asText();
              log.trace("set iri: " + iri);
            }
          }
        }
      }
    }
    return Optional.ofNullable(iri);
  }

  public Optional<OlsResult> queryForOlsObject(String shortcode) {
    UriComponents uriComponents =
        UriComponentsBuilder.fromUriString(
                bioSamplesProperties.getOls() + "/api/terms?id={shortcode}&size=500")
            .build();
    URI uri = uriComponents.expand(shortcode).encode().toUri();

    log.trace("OLS query for shortcode {} against {}", shortcode, uri);

    RequestEntity<Void> requestEntity =
        RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
    ResponseEntity<ObjectNode> responseEntity =
        restTemplate.exchange(requestEntity, new ParameterizedTypeReference<ObjectNode>() {});

    // non-200 status code or empty body
    if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
      log.trace(
          "Http status: {}, Body: {}",
          responseEntity.getStatusCodeValue(),
          responseEntity.getBody());
      return Optional.empty();
    }

    ObjectNode node = responseEntity.getBody();

    OlsResult olsResult = null;
    if (node.has("_embedded") && node.get("_embedded").has("terms")) {
      for (JsonNode term : node.get("_embedded").get("terms")) {
        if (term.has("iri") && term.has("is_defining_ontology")) {
          // if we don't have an iri use this but if there is a defining ontoloy use this
          // in
          // preference
          if (olsResult == null || term.get("is_defining_ontology").booleanValue()) {
            // if this term is obsolete get the most upto-date one
            if (term.get("is_obsolete").booleanValue()) {
              String[] replaceTermArray = term.get("term_replaced_by").asText().split("/");
              String replaceTerm = replaceTermArray[replaceTermArray.length - 1];
              return queryForOlsObject(replaceTerm);
            }

            olsResult = new OlsResult(term.get("label").asText(), term.get("iri").asText());
          }
        }
      }
    }
    return Optional.ofNullable(olsResult);
  }
}
