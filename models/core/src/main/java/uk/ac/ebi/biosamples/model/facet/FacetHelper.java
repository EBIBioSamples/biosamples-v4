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

import java.util.ArrayList;
import java.util.List;

public class FacetHelper {
  private FacetHelper() {
    // hide constructor
  }

  public static final List<String> FACETING_FIELDS;
  public static final List<String> RANGE_FACETING_FIELDS; // we are only supporting date range facets now
  public static final List<String> IGNORE_FACETING_FIELDS;

  static {
    FACETING_FIELDS = new ArrayList<>();
    FACETING_FIELDS.add("organism");
    FACETING_FIELDS.add("external reference");
    FACETING_FIELDS.add("sex");
    FACETING_FIELDS.add("tissue");
    FACETING_FIELDS.add("strain");
    FACETING_FIELDS.add("organism part");
    FACETING_FIELDS.add("cell type");
    FACETING_FIELDS.add("isolate");
    FACETING_FIELDS.add("sample type");
    FACETING_FIELDS.add("genotype");
    FACETING_FIELDS.add("isolation source");
    FACETING_FIELDS.add("histological type");
    FACETING_FIELDS.add("age");
    FACETING_FIELDS.add("host");
    FACETING_FIELDS.add("latitude and longitude");
    FACETING_FIELDS.add("environmental medium");
    FACETING_FIELDS.add("biomaterial provider");
    FACETING_FIELDS.add("development stage");
    FACETING_FIELDS.add("investigation type");
    FACETING_FIELDS.add("disease state");
    FACETING_FIELDS.add("cell line");
    FACETING_FIELDS.add("treatment");
    FACETING_FIELDS.add("depth");
    FACETING_FIELDS.add("host sex");
    FACETING_FIELDS.add("cultivar");
    FACETING_FIELDS.add("elevation");
    FACETING_FIELDS.add("host disease");
    FACETING_FIELDS.add("developmental stage");
    FACETING_FIELDS.add("disease");
    FACETING_FIELDS.add("host age");
    FACETING_FIELDS.add("phenotype");
    FACETING_FIELDS.add("breed");
    FACETING_FIELDS.add("collection date");
    FACETING_FIELDS.add("geographic location");
    FACETING_FIELDS.add("data use conditions");
  }

  static {
    RANGE_FACETING_FIELDS = new ArrayList<>();
    RANGE_FACETING_FIELDS.add("release");
  }

  static {
    IGNORE_FACETING_FIELDS = new ArrayList<>();
    IGNORE_FACETING_FIELDS.add("description");
    IGNORE_FACETING_FIELDS.add("title");
    IGNORE_FACETING_FIELDS.add("NCBI submission model");
    IGNORE_FACETING_FIELDS.add("External Id");
    IGNORE_FACETING_FIELDS.add("INSDC last update");
    IGNORE_FACETING_FIELDS.add("INSDC first public");
    IGNORE_FACETING_FIELDS.add("INSDC status");
    IGNORE_FACETING_FIELDS.add("INSDC center name");
    IGNORE_FACETING_FIELDS.add("INSDC center alias");
    IGNORE_FACETING_FIELDS.add("description title");
    IGNORE_FACETING_FIELDS.add("gap subject id");
    IGNORE_FACETING_FIELDS.add("gap accession");
    IGNORE_FACETING_FIELDS.add("gap sample id");
    IGNORE_FACETING_FIELDS.add("external ID");
    IGNORE_FACETING_FIELDS.add("submitted subject id");
    IGNORE_FACETING_FIELDS.add("submitted sample id");
    IGNORE_FACETING_FIELDS.add("submitter handle");
    IGNORE_FACETING_FIELDS.add("biospecimen repository sample id");
  }

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
