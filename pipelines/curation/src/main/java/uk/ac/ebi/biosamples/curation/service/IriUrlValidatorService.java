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
package uk.ac.ebi.biosamples.curation.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Service to validate if an IRI URL resolves */
@Service
public class IriUrlValidatorService {
  private static final String OBO = "purl.obolibrary.org/obo";
  private static final String EBIUK = "www.ebi.ac.uk";

  public IriUrlValidatorService() {}

  // @Cacheable(value = "iri")
  public boolean validateIri(final String iri) {
    try {
      return checkHttpStatusOfUrl(iri);
    } catch (final IOException e) {
      return false;
    }
  }

  private boolean checkHttpStatusOfUrl(final String urlToCheck) throws IOException {
    final URL url = new URL(urlToCheck);
    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setConnectTimeout(5000);
    final int response;

    conn.setRequestMethod("GET");
    conn.connect();
    response = conn.getResponseCode();

    return HttpStatus.valueOf(response).is2xxSuccessful();
  }

  public boolean checkUrlForPattern(final String displayIri) {
    if (displayIri.contains(OBO) || displayIri.contains(EBIUK)) {
      return false;
    } else {
      return true;
    }
  }
}
