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
package uk.ac.ebi.biosamples.core.service;

import java.util.Optional;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.core.model.ExternalReference;

public class ExternalReferenceUtils {
  private ExternalReferenceUtils() {
    // private
  }

  private static final String ENA_BASE_URL_FRAGMENT = "www.ebi.ac.uk/ena";
  private static final String ARRAYEXPRESS_BASE_URL_FRAGMENT = "www.ebi.ac.uk/arrayexpress";
  private static final String HPSCREG_URL_FRAGMENT = "hpscreg.eu/";
  private static final String DBGAP_BASE_URL_FRAGMENT = "ncbi.nlm.nih.gov/projects/gap";
  private static final String EGA_DATASET_BASE_URL_FRAGMENT = "ega-archive.org/datasets";
  private static final String EGA_SAMPLE_BASE_URL_FRAGMENT = "ega-archive.org/metadata";
  private static final String EGA_STUDY_BASE_URL_FRAGMENT = "ega-archive.org/studies";
  private static final String BIOSTUDIES_BASE_URL_FRAGMENT = "ebi.ac.uk/biostudies";
  private static final String EVA_BASE_URL_FRAGMENT = "ebi.ac.uk/eva";
  private static final String DUO_BASE_URL = "http://purl.obolibrary.org/obo/";

  public static String getNickname(final ExternalReference externalReference) {
    if (externalReference.getUrl().contains(ENA_BASE_URL_FRAGMENT)) {
      return "ENA";
    } else if (externalReference.getUrl().contains(ARRAYEXPRESS_BASE_URL_FRAGMENT)) {
      return "ArrayExpress";
    } else if (externalReference.getUrl().contains(HPSCREG_URL_FRAGMENT)) {
      return "hPSCreg";
    } else if (externalReference.getUrl().contains(DBGAP_BASE_URL_FRAGMENT)) {
      return "dbGaP";
    } else if (externalReference.getUrl().contains(EGA_DATASET_BASE_URL_FRAGMENT)) {
      return "EGA Dataset";
    } else if (externalReference.getUrl().contains(EGA_SAMPLE_BASE_URL_FRAGMENT)) {
      return "EGA Sample";
    } else if (externalReference.getUrl().contains(EGA_STUDY_BASE_URL_FRAGMENT)) {
      return "EGA Study";
    } else if (externalReference.getUrl().contains(BIOSTUDIES_BASE_URL_FRAGMENT)) {
      return "BioStudies";
    } else if (externalReference.getUrl().contains(EVA_BASE_URL_FRAGMENT)) {
      return "EVA";
    } else {
      return "other";
    }
  }

  public static Optional<String> getDataId(final ExternalReference externalReference) {
    final String nickname = getNickname(externalReference);
    if ("ENA".equals(nickname)
        || "ArrayExpress".equals(nickname)
        || "hPSCreg".equals(nickname)
        || "EGA Dataset".equals(nickname)
        || "EGA Sample".equals(nickname)
        || "EGA Study".equals(nickname)
        || "BioStudies".equals(nickname)) {
      final UriComponents uriComponents =
          UriComponentsBuilder.fromHttpUrl(externalReference.getUrl()).build();
      final String lastPathSegment =
          uriComponents.getPathSegments().get(uriComponents.getPathSegments().size() - 1);
      return Optional.of(lastPathSegment);
    } else if ("dbGaP".equals(nickname)) {
      final UriComponents uriComponents =
          UriComponentsBuilder.fromHttpUrl(externalReference.getUrl()).build();
      final String studyId = uriComponents.getQueryParams().getFirst("study_id");
      return Optional.of(studyId);
    } else if ("EVA".equals(nickname)) {
      final UriComponents uriComponents =
          UriComponentsBuilder.fromHttpUrl(externalReference.getUrl()).build();
      final String evaStudyId = uriComponents.getQueryParams().getFirst("eva-study");
      return Optional.of(evaStudyId);
    }
    return Optional.empty();
  }

  static String getDuoUrl(final String duoCode) {
    return DUO_BASE_URL + duoCode.replace(":", "_");
  }
}
