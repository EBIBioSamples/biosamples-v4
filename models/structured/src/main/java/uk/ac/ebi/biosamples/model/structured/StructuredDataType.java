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
package uk.ac.ebi.biosamples.model.structured;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum StructuredDataType {
  AMR(Collections.emptyList()),
  CHICKEN_DATA(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner", "Method")),
  HISTOLOGY_MARKERS(
      Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner", "Method")),
  MOLECULAR_MARKERS(
      Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner", "Method")),
  FATTY_ACIDS(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner", "Method")),
  HEAVY_METALS(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner", "Method")),
  SALMON_DATA(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner", "Method"));

  private final List<String> headers;

  StructuredDataType(List<String> headers) {
    this.headers = headers;
  }

  public List<String> getHeaders() {
    return headers;
  }
}
