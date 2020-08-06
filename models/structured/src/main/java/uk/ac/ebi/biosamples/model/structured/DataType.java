package uk.ac.ebi.biosamples.model.structured;

import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum DataType {
    AMR(Collections.emptyList()),
    HISTOLOGY(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner")),
    CHICKEN_DATA(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner")),
    HISTOLOGY_MARKERS(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner")),
    MOLECULAR_MARKERS(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner")),
    FATTY_ACCIDS(Arrays.asList("Marker", "Measurement", "Measurement Units", "Partner"));

    private final List<String> headers;
    DataType(List<String> headers) {
        this.headers = headers;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public static DataType getDataTypeFromClass(Class clz) {
        if (clz == HistologyEntry.class) {
            return HISTOLOGY;
        } else if (clz == AMREntry.class) {
            return AMR;
        }

        return null;
    }
}
