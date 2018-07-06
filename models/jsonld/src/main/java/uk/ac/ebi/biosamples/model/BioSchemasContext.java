package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.ac.ebi.biosamples.service.ContextDeserializer;
import uk.ac.ebi.biosamples.service.ContextSerializer;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = ContextSerializer.class)
@JsonDeserialize(using = ContextDeserializer.class)
public class BioSchemasContext {

    private final URI schemaOrgContext= URI.create("http://schema.org");
    private final URI schemaOrgBaseContext = schemaOrgContext;
    private final Map<String, URI> otherContexts;

    public BioSchemasContext() {
        this.otherContexts = new HashMap<>();
    }

    public BioSchemasContext(Map<String, URI> otherContexts) {
        this.otherContexts = otherContexts;
    }

    public void addContext(String name, URI id) {
        this.otherContexts.put(name, id);
    }

    public URI getSchemaOrgContext() {
        return schemaOrgContext;
    }

    public URI getBaseContext() {
        return schemaOrgBaseContext;
    }

    public Map<String, URI> getOtherContext() {
        return otherContexts;
    }
}
