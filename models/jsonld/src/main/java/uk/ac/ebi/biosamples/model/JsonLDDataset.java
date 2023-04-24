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
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    value = {"@context", "@type", "name", "description", "url", "includedInDataCatalog"})
public class JsonLDDataset implements BioschemasObject {

  @JsonProperty("@context")
  private final String context = "http://schema.org";

  @JsonProperty("@type")
  private final String type = "Dataset";

  @JsonProperty("name")
  private final String name = "EMBL-EBI BioSamples";

  @JsonProperty("description")
  private final String description =
      "The BioSamples database at EMBL-EBI provides a central hub for sample metadata storage and linkage to other EMBL-EBI resources. BioSamples contains just over 5 million samples in 2018. Fast, reciprocal data exchange is fully established between sister Biosample databases and other INSDC partners, enabling a worldwide common representation and centralisation of sample metadata. BioSamples plays a vital role in data coordination, acting as the sample metadata repository for many biomedical projects including the Functional Annotation of Animal Genomes (FAANG), the European Bank for induced pluripotent Stem Cells (EBiSC) and the {Human Induced Pluripotent Stem Cell Initiative (HipSci). Data growth is driven by the ever more important role BioSamples is playing as an ELIXIR data deposition database and as the EMBL-EBI hub for sample metadata. BioSamples is now the destination for samples from ELIXIR-EXCELERATE use case projects ranging from plant phenotyping to marine metagenomics, as well as several other community efforts including the Global Alliance for Genomics and Health (GA4GH) and the ELIXIR Bioschemas project.";

  private String url = "https://www.ebi.ac.uk/biosamples/samples";

  private Map<String, String> dataCatalog = getDefaultDataCatalog();

  private Map getDefaultDataCatalog() {
    final Map<String, String> dataCatalog = new HashMap<>();
    dataCatalog.put("@type", "DataCatalog");
    dataCatalog.put("@id", "https://www.ebi.ac.uk/biosamples");
    return dataCatalog;
  }

  @JsonProperty("url")
  public String getUrl() {
    return url;
  }

  public JsonLDDataset datasetUrl(final String url) {
    this.url = url;
    return this;
  }

  @JsonProperty("includedInDataCatalog")
  public Map<String, String> getDataCatalog() {
    return dataCatalog;
  }

  public JsonLDDataset dataCatalogUrl(final String dataCatalogUrl) {
    final Map dataCatalog = getDefaultDataCatalog();
    dataCatalog.put("@id", dataCatalogUrl);
    this.dataCatalog = dataCatalog;
    return this;
  }
}
