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
package uk.ac.ebi.biosamples.utils.phenopacket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * OLSDAtaRetriever is api class for working with EBI OLS. It fetchs specific (see methods of class)
 * metadata for phenopackets.
 *
 * @author Dilshat Salikhov
 */
@Service
public class OLSDataRetriever {
  private JsonNode node;
  private final Map<String, String> ontologyPrefixMapping = new HashMap<>();

  public OLSDataRetriever() {
    ontologyPrefixMapping.put("orphanet", "ordo");
  }
  /**
   * Read json from OLS by iri provided in GA4GH sample
   *
   * @param iri
   */
  void readOntologyJsonFromUrl(final String iri) {
    final String linkToTerm;
    try {
      // TODO move to application properties
      linkToTerm = "https://www.ebi.ac.uk/ols/api/terms?iri=" + URLEncoder.encode(iri, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    readJson(linkToTerm);
  }

  /**
   * Read json from OLS by ontology id, for example 'efo'
   *
   * @param id
   */
  void readResourceInfoFromUrl(String id) {

    // Necessary for ontologies like orphanet, where the prefix is not orphanet but ordo
    if (ontologyPrefixMapping.containsKey(id.toLowerCase())) {
      id = ontologyPrefixMapping.get(id.toLowerCase());
    }

    String linkToResourceInfo = null;
    // TODO move to application properties
    try {
      linkToResourceInfo =
          "https://www.ebi.ac.uk/ols/api/ontologies/"
              + URLEncoder.encode(id.toLowerCase(), "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    readJson(linkToResourceInfo);
  }

  /**
   * Fetchs json from given url and store it as Json tree in node attribute
   *
   * @param link link to OLS element
   */
  private void readJson(final String link) {
    final URL urlToTerm;
    try {
      urlToTerm = new URL(link);
    } catch (final MalformedURLException e) {
      throw new RuntimeException(e);
    }
    final ObjectMapper mapper = new ObjectMapper();
    try {
      node = mapper.readTree(urlToTerm);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  String getOntologyTermId() {
    JsonNode terms = node.get("_embedded").get("terms");
    terms = terms.get(0);
    return terms.get("obo_id").asText();
  }

  String getOntologyTermLabel() {
    JsonNode terms = node.get("_embedded").get("terms");
    terms = terms.get(0);
    return terms.get("label").asText();
  }

  String getResourceId() {
    return node.get("ontologyId").asText();
  }

  String getResourceName() {
    final JsonNode config = node.get("config");
    return config.get("title").isNull()
        ? config.get("localizedTitles").get("en").asText()
        : config.get("title").asText();
  }

  String getResourcePrefix() {
    final JsonNode config = node.get("config");
    return config.get("preferredPrefix").asText();
  }

  String getResourceUrl() {
    final JsonNode config = node.get("config");
    return config.get("fileLocation").asText();
  }

  String getResourceVersion() {
    final JsonNode config = node.get("config");
    return config.get("version").asText();
  }
}
