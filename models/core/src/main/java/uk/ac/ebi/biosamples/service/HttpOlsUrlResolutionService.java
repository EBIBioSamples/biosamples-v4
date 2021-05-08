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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.SortedSet;

@Service
public class HttpOlsUrlResolutionService {
  public static final String OLS_PREFIX = "http://www.ebi.ac.uk/ols/terms?iri=";
  public static final String OBO = "purl.obolibrary.org/obo";
  public static final String EBIUK = "www.ebi.ac.uk";
  public static Logger log = LoggerFactory.getLogger(HttpOlsUrlResolutionService.class);

  public HttpOlsUrlResolutionService() {}

  /**
   * This returns a string representation of the URL to lookup the associated ontology term iri in
   * EBI OLS.
   *
   * @return url representation of the IRI
   */
  // @Cacheable(value = "iri")
  public String getIriOls(final SortedSet<String> iri) {
    if (iri == null || iri.size() == 0) return null;

    String displayIri = iri.first();

    // check this is a sane iri
    try {
      final UriComponents iriComponents =
              UriComponentsBuilder.fromUriString(displayIri).build(true);

      if (iriComponents.getScheme() == null
              || iriComponents.getHost() == null
              || iriComponents.getPath() == null) {
        // incomplete iri (e.g. 9606, EFO_12345) don't bother to check
        return null;
      }

      // TODO application.properties this
      // TODO use https
      final String iriUrl = URLEncoder.encode(displayIri, StandardCharsets.UTF_8.toString());

      if (checkUrlForPattern(displayIri)) {
        return OLS_PREFIX + iriUrl;
      } else {
        return displayIri;
      }
    } catch (final Exception e) {
      // FIXME: Can't use a non static logger here because
      log.error("An error occurred while trying to build OLS iri for " + displayIri, e);
      return null;
    }
  }

  public boolean checkUrlForPattern(final String displayIri) {
    if (displayIri.contains(OBO) || displayIri.contains(EBIUK)) return true;
    else return false;
  }
}
