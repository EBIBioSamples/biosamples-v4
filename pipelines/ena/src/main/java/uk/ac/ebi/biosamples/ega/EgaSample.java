package uk.ac.ebi.biosamples.ega;

import lombok.Data;

@Data
public class EgaSample {
    private String egaId;
    private String biosampleId;
    private String subjectId;
    private String gender;
    private String phenotype;
}
