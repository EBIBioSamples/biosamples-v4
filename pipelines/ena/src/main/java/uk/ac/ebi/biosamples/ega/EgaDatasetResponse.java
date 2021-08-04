package uk.ac.ebi.biosamples.ega;

import lombok.Data;

import java.util.List;

@Data
public class EgaDatasetResponse {
    private String stableId;
    private List<DataUseCondition> dataUseConditions;
}

@Data
class DataUseCondition {
    private String ontology;
    private String code;
    private String modifier;
    private String label;
}
