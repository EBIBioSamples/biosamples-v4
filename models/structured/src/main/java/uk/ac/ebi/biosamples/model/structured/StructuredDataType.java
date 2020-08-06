package uk.ac.ebi.biosamples.model.structured;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum StructuredDataType {
    AMR(Collections.emptyList()),
    CHICKEN_DATA(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner")),
    HISTOLOGY_MARKERS(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner")),
    MOLECULAR_MARKERS(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner")),
    FATTY_ACCIDS(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner"));

    private final List<String> headers;

    StructuredDataType(List<String> headers) {
        this.headers = headers;
    }

    public List<String> getHeaders() {
        return headers;
    }
}
