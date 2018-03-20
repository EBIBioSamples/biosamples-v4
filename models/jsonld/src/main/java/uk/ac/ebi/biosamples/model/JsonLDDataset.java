package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(value={"@context", "@type", "name", "description", "url", "includedInDataCatalog"})
public class JsonLDDataset {

    @JsonProperty("@context")
    private final String context = "http://schema.org";

    @JsonProperty("@type")
    private final String type = "Dataset";

    @JsonProperty("name")
    private final String name = "Sample collection";

    @JsonProperty("description")
    private final String description = "BioSamples stores and supplies descriptions and metadata about biological samples used in research and development by academia and industry. Samples are either 'reference' samples (e.g. from 1000 Genomes, HipSci, FAANG) or have been used in an assay database such as the European Nucleotide Archive (ENA) or ArrayExpress.";

    //TODO Use relative application url and not hard-coded one
    @JsonProperty("url")
    private final String url =  "https://www.ebi.ac.uk/biosamples/samples";

    @JsonProperty("includedInDataCatalog")
    private final Map<String, String> dataCatalog = getDataCatalog();


    private Map getDataCatalog() {
        Map<String, String> dataCatalog = new HashMap<>();
        dataCatalog.put("@type", "DataCatalog");
        //TODO Use relative application url and not hard-coded one
        dataCatalog.put("@id", "https://www.ebi.ac.uk/biosamples");
        return dataCatalog;
    }

}

