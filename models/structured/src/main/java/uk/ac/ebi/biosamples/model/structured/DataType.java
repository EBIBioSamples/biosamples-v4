package uk.ac.ebi.biosamples.model.structured;

import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;

public enum DataType {
    AMR,
    HISTOLOGY;

    public static DataType getDataTypeFromClass(Class clz) {
        if (clz == StructuredEntry.class) {
            return HISTOLOGY;
        } else if (clz == AMREntry.class) {
            return AMR;
        }

        return null;
    }
}
