package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(value={"@context", "@type", "name", "description", "url", "includedInDataCatalog"})
public class JsonLDDataset implements BioschemasObject{

    @JsonProperty("@context")
    private final String context = "http://schema.org";

    @JsonProperty("@type")
    private final String type = "Dataset";

    @JsonProperty("name")
    private final String name = "Sample collection";

    @JsonProperty("description")
    private final String description = "BioSamples stores and supplies descriptions and metadata about biological samples used in research and development by academia and industry. Samples are either 'reference' samples (e.g. from 1000 Genomes, HipSci, FAANG) or have been used in an assay database such as the European Nucleotide Archive (ENA) or ArrayExpress.";

    private String url =  "https://www.ebi.ac.uk/biosamples/samples";

    private Map<String, String> dataCatalog = getDefaultDataCatalog();


    private Map getDefaultDataCatalog() {
        Map<String, String> dataCatalog = new HashMap<>();
        dataCatalog.put("@type", "DataCatalog");
        dataCatalog.put("@id", "https://www.ebi.ac.uk/biosamples");
        return dataCatalog;
    }


    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    public JsonLDDataset datasetUrl(String url) {
        this.url = url;
        return this;
    }

    @JsonProperty("includedInDataCatalog")
    public Map<String, String> getDataCatalog() {
        return dataCatalog;
    }

    public JsonLDDataset dataCatalogUrl(String dataCatalogUrl) {
        Map dataCatalog = getDefaultDataCatalog();
        dataCatalog.put("@id", dataCatalogUrl);
        this.dataCatalog = dataCatalog;
        return this;
    }
}

