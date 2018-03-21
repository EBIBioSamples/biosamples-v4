package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(value = {"@context",  "@type", "description", "keywords", "name", "url", "url", "publication", "provider", "sourceOrganization"})
public class JsonLDDataCatalog implements BioschemasObject{

    @JsonProperty("@context")
    private final String context = "http://schema.org";

    @JsonProperty("@type")
    private final String type = "DataCatalog";


    @JsonProperty("description")
    private final String description;

    @JsonProperty("keywords")
    private final String keywords;

    @JsonProperty("provider")
    private final Map<String, String> provider;

    @JsonProperty("name")
    private final String name;

    private String url;

    private List<Map<String,String>> dataset;

    @JsonProperty("publication")
    private final List<Map<String,String>> publication;

    @JsonProperty("sourceOrganization")
    private final Map<String, String> sourceOrganization;

    public JsonLDDataCatalog() {
        this.description = "BioSamples stores and supplies descriptions and metadata about biological samples " +
                "used in research and development by academia and industry. " +
                "Samples are either 'reference' samples (e.g. from 1000 Genomes, HipSci, FAANG) " +
                "or have been used in an assay database such as the European Nucleotide Archive (ENA) or ArrayExpress.";
        this.keywords = "samples, sample metadata";
        this.provider = getBiosamplesProvider();
        this.name = "BioSamples database";
        this.url = "https://www.ebi.ac.uk/biosamples";
        this.dataset = getDefaultBioSamplesDataset();
        this.publication = getBioSamplesPublication();
        this.sourceOrganization = getBioSamplesSourceOrganization();

    }

    private Map<String,String> getBioSamplesSourceOrganization() {
        Map<String, String> sourceOrganization = new HashMap<>();
        sourceOrganization.put("@type", "Organization");
        sourceOrganization.put("name", "The European Bioinformatics Institute (EMBL-EBI)");
        sourceOrganization.put("url", "https://www.ebi.ac.uk/");
        return sourceOrganization;
    }

    private List<Map<String,String>> getBioSamplesPublication() {
        List<Map<String, String>> publication = new ArrayList<>();
        Map<String, String> publicationEntry = new HashMap<>();
        publicationEntry.put("@type", "PublicationEvent");
        publicationEntry.put("name", "Updates to BioSamples database at European Bioinformatics Institute");
        publicationEntry.put("url", "http://identifiers.org/pubmed:24265224");
        publication.add(publicationEntry);
        return publication;
    }

    private List<Map<String,String>> getDefaultBioSamplesDataset() {
        List<Map<String, String>> dataset = new ArrayList<Map<String,String>>();
        Map<String, String> datasetEntry = new HashMap<String, String>();
        datasetEntry.put("@type", "Dataset");
        datasetEntry.put("@id", "https://www.ebi.ac.uk/biosamples/samples");
        dataset.add(datasetEntry);
        return dataset;
    }

    private Map<String, String> getBiosamplesProvider() {
        Map<String,String> provider = new HashMap<>();
        provider.put("@type", "Organization");
        provider.put("name", "BioSamples");
        provider.put("email",  "biosamples@ebi.ac.uk");
        return provider;
    }

    @JsonProperty("provider")
    private final Map<String, String> getProvider() {
        return this.provider;
    }


    public List<Map<String, String>> getDataset() {
        return dataset;
    }

    @JsonProperty("dataset")
    public JsonLDDataCatalog datasetUrl(String datasetUrl) {
        List<Map<String, String>> allDatasets = getDefaultBioSamplesDataset();
        Map dataset = allDatasets.get(0);
        dataset.put("@id", datasetUrl);
        allDatasets = new ArrayList<>();
        allDatasets.add(dataset);
        this.dataset = allDatasets;
        return this;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    public JsonLDDataCatalog url(String url) {
        this.url = url;
        return this;
    }
}
