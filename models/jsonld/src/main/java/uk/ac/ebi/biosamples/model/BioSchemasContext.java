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
package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import uk.ac.ebi.biosamples.service.ContextDeserializer;
import uk.ac.ebi.biosamples.service.ContextSerializer;

@JsonSerialize(using = ContextSerializer.class)
@JsonDeserialize(using = ContextDeserializer.class)
public class BioSchemasContext {

  private final URI schemaOrgContext;
  private final Map<String, URI> otherContexts;

  public BioSchemasContext() {
    schemaOrgContext = URI.create("http://schema.org");
    otherContexts = new HashMap<>();
    otherContexts.put("OBI", URI.create("http://purl.obolibrary.org/obo/OBI_"));
    otherContexts.put("biosample", URI.create("http://identifiers.org/biosample/"));
  }

  public URI getSchemaOrgContext() {
    return schemaOrgContext;
  }

  public Map<String, URI> getOtherContexts() {
    return otherContexts;
  }

  public void addOtherContexts(String name, URI id) {
    this.otherContexts.put(name, id);
  }
}
