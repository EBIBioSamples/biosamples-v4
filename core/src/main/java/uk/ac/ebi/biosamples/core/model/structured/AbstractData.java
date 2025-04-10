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
package uk.ac.ebi.biosamples.core.model.structured;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ebi.biosamples.core.service.structured.AbstractDataDeserializer;

@Data
@NoArgsConstructor
@JsonPropertyOrder({"domain", "type", "schema", "content"})
@JsonDeserialize(using = AbstractDataDeserializer.class)
public abstract class AbstractData implements Comparable<AbstractData> {
  @JsonProperty("domain")
  public abstract String getDomain();

  @JsonProperty("webinSubmissionAccountId")
  public abstract String getWebinSubmissionAccountId();

  @JsonProperty("type")
  public abstract StructuredDataType getDataType();

  @JsonProperty("schema")
  public abstract URI getSchema();

  @JsonProperty("content")
  public abstract Object getStructuredData();

  @JsonIgnore
  public abstract List<String> getHeaders();

  @JsonIgnore
  public abstract List<Map<String, StructuredCell>> getDataAsMap();
}
