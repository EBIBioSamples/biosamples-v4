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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.SortedSet;

@Service
public class HttpOlsUrlResolutionService {
  private static final String OLS_PREFIX = "https://www.ebi.ac.uk/ols4?termId=";
  private static final String OBO_URL_SEGMENT = "purl.obolibrary.org/obo";
  private static final String EBI_URL_SEGMENT = "www.ebi.ac.uk";
  public static Logger log = LoggerFactory.getLogger(HttpOlsUrlResolutionService.class);

  public HttpOlsUrlResolutionService() {
  }

  public static boolean isCurie(String term) {
    String[] segments;
    if (term.contains(":")) {
      segments = term.split(":");
    } else if (term.contains("_")) {
      segments = term.split("_");
    } else {
      return false;
    }

    return segments.length == 2 && segments[0].matches("[a-zA-Z]+") && segments[1].matches("\\d+");
  }

  private static String formatCurie(String displayIri) {
    return displayIri.trim().replace("_", ":");
  }

  /**
   * This returns a string representation of the URL to lookup the associated ontology term iri in
   * EBI OLS.
   *
   * @return url representation of the IRI
   */
  // @Cacheable(value = "iri")
  public String getIriOls(final SortedSet<String> iri) {
    if (iri == null || iri.size() == 0) {
      return null;
    }

    final String displayIri = iri.first();

    if (isCurie(displayIri)) {
      return formatCurie(displayIri);
    }

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

      String[] segments = displayIri.split("/");
      String curie = segments[segments.length - 1].replace("_", ":");

      if (checkUrlForPattern(displayIri)) {
        return OLS_PREFIX + curie;
      } else {
        return displayIri;
      }
    } catch (final Exception e) {
      // FIXME: Can't use a non static logger here because
      log.error("An error occurred while trying to build OLS iri for " + displayIri, e);
      return null;
    }
  }

  private boolean checkUrlForPattern(final String displayIri) {
    return displayIri.contains(OBO_URL_SEGMENT) || displayIri.contains(EBI_URL_SEGMENT);
  }
}
