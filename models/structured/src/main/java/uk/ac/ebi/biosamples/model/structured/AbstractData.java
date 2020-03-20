package uk.ac.ebi.biosamples.model.structured;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.ac.ebi.biosamples.service.structured.AbstractDataDeserializer;

import java.net.URI;


@JsonPropertyOrder({"domain", "type", "schema", "content"})
@JsonDeserialize(using = AbstractDataDeserializer.class)
public abstract class AbstractData implements Comparable<AbstractData> {
    public AbstractData() {
    }

    @JsonProperty
    public abstract String getDomain();

    @JsonProperty("type")
    public abstract DataType getDataType();

    @JsonProperty("schema")
    public abstract URI getSchema();

    @JsonProperty("content")
    public abstract Object getStructuredData();
}
