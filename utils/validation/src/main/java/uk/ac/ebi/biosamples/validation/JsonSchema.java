package uk.ac.ebi.biosamples.validation;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonSchema {
    private String id;
    private String name;
    private String version;
    private String title;
    private String description;
    private String domain;
    private String metaSchema;
    private JsonNode schema;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getMetaSchema() {
        return metaSchema;
    }

    public void setMetaSchema(String metaSchema) {
        this.metaSchema = metaSchema;
    }

    public JsonNode getSchema() {
        return schema;
    }

    public void setSchema(JsonNode schema) {
        this.schema = schema;
    }
}
