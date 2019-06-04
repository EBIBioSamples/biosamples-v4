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

    private final URI schemaOrgContext;
    private final Map<String, URI> otherContexts;

    public BioSchemasContext() {
        schemaOrgContext = URI.create("http://schema.org");
        otherContexts = new HashMap<>();
        otherContexts.put("OBI", URI.create("http://purl.obolibrary.org/obo/OBI_"));
        otherContexts.put("biosample", URI.create("http://identifiers.org/biosample/"));
    }

    public URI getSchemaOrgContext() {
        return schemaOrgContext;
    }

    public Map<String, URI> getOtherContexts() {
        return otherContexts;
    }

    public void addOtherContexts(String name, URI id) {
        this.otherContexts.put(name, id);
    }
}
