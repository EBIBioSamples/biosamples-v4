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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;

/** Object representing BioSchema Sample entity */
@JsonPropertyOrder({
  "@id",
  "@context",
  "@type",
  "additionalType",
  "identifier",
  "name",
  "description",
  "url",
  "subjectOf",
  "additionalProperty"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonLDSample implements BioschemasObject {

  private final URI sampleOntologyURI = URI.create("http://purl.obolibrary.org/obo/OBI_0000747");

  //    @JsonProperty("@context")
  //    private final BioSchemasContext sampleContext = new BioSchemasContext();

  @JsonProperty("@type")
  private final String[] type = {"Sample", "OBI:0000747"};

  // private final String additionalType =
  // "http://www.ontobee.org/ontology/OBI?iri=http://purl.obolibrary.org/obo/OBI_0000747";
  private String id;
  private String sameAs;
  private String[] identifiers;
  private String name;
  private String description;
  private String url;
  //    private final URI additionalType =
  // URI.create("http://purl.obolibrary.org/obo/OBI_0000747");

  private List<String> subjectOf;

  @JsonProperty("additionalProperty")
  private List<JsonLDPropertyValue> additionalProperties;

  //    @JsonIgnore
  //    public BioSchemasContext getContext() {
  //        return sampleContext;
  //    }

  public String[] getType() {
    return type;
  }

  //    public String getAdditionalType() {
  //        return additionalType;
  //    }

  @JsonProperty("@id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("sameAs")
  public String getSameAs() {
    return sameAs;
  }

  public void setSameAs(String sameAs) {
    this.sameAs = sameAs;
  }

  @JsonProperty("identifier")
  public String[] getIdentifiers() {
    return identifiers;
  }

  public void setIdentifiers(String[] identifiers) {
    this.identifiers = identifiers;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public List<String> getSubjectOf() {
    return subjectOf;
  }

  public void setSubjectOf(List<String> subjectOf) {
    this.subjectOf = subjectOf;
  }

  public List<JsonLDPropertyValue> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(List<JsonLDPropertyValue> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  //    public URI getAdditionalType() {
  //        return additionalType;
  //    }

}
