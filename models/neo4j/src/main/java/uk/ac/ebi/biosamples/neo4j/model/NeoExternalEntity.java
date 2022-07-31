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
package uk.ac.ebi.biosamples.neo4j.model;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.service.ExternalReferenceUtils;

public class NeoExternalEntity {
  private String url;
  private String archive;
  private String ref;

  private NeoExternalEntity(String url, String archive, String ref) {
    this.url = url;
    this.archive = archive;
    this.ref = ref;
  }

  public String getUrl() {
    return url;
  }

  public String getArchive() {
    return archive;
  }

  public String getRef() {
    return ref;
  }

  public static NeoExternalEntity build(ExternalReference reference) {
    String externalRef = ExternalReferenceUtils.getNickname(reference).toLowerCase();
    return new NeoExternalEntity(
        reference.getUrl(),
        externalRef.startsWith("ega") ? "ega" : externalRef,
        ExternalReferenceUtils.getDataId(reference)
            .orElse(String.valueOf(Math.abs(reference.getUrl().hashCode()))));
  }
}
