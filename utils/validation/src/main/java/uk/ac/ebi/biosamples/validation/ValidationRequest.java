package uk.ac.ebi.biosamples.validation;

import com.fasterxml.jackson.databind.JsonNode;

public class ValidationRequest {
    private JsonNode schema;
    private JsonNode object;

    public ValidationRequest(JsonNode schema, JsonNode object) {
        this.schema = schema;
        this.object = object;
    }

    public JsonNode getSchema() {
        return schema;
    }

    public void setSchema(JsonNode schema) {
        this.schema = schema;
    }

    public JsonNode getObject() {
        return object;
    }

    public void setObject(JsonNode object) {
        this.object = object;
    }
}
