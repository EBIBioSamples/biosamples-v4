package uk.ac.ebi.biosamples.model.structured;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public abstract class StructuredEntry {
    @JsonIgnore
    public abstract Map<String, StructuredCell> getDataAsMap();
}
