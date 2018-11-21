package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.ac.ebi.biosamples.service.ContextSerializer;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

//@JsonSerialize(using = ContextSerializer.class)
public class BioSchemasContext {

    private final URI schemaOrgBaseContext = URI.create("http://schema.org");

    @JsonProperty("@base")
    public URI getBaseContext() {
        return schemaOrgBaseContext;
    }

}
