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
package uk.ac.ebi.biosamples.model.facet;

import java.util.List;

public class FacetHelper {
  private FacetHelper() {
    // hide constructor
  }

  public static final List<String> FACETING_FIELDS =
      List.of(
          "organism",
          "external reference",
          "sex",
          "tissue",
          "strain",
          "organism part",
          "cell type",
          "isolate",
          "sample type",
          "genotype",
          "isolation source",
          "histological type",
          "age",
          "host",
          "latitude and longitude",
          "environmental medium",
          "biomaterial provider",
          "development stage",
          "investigation type",
          "disease state",
          "cell line",
          "treatment",
          "depth",
          "host sex",
          "cultivar",
          "elevation",
          "host disease",
          "developmental stage",
          "disease",
          "host age",
          "phenotype",
          "breed",
          "collection date",
          "geographic location",
          "data use conditions");
  public static final List<String> RANGE_FACETING_FIELDS =
      List.of("release"); // we are only supporting date range facets now

  public static String get_encoding_suffix(String attribute) {
    String suffix = "";
    if (FACETING_FIELDS.contains(attribute)) {
      suffix = "_av_ss";
    } else if (RANGE_FACETING_FIELDS.contains(attribute)) {
      suffix = "_dt";
    }
    return suffix;
  }

  public static int compareFacets(String f1, String f2) {
    if (!FACETING_FIELDS.contains(f1) && !FACETING_FIELDS.contains(f2)) {
      return 0;
    } else if (!FACETING_FIELDS.contains(f1)) {
      return -1;
    } else if (!FACETING_FIELDS.contains(f2)) {
      return 1;
    }

    return FACETING_FIELDS.indexOf(f2) - FACETING_FIELDS.indexOf(f1);
  }
}
