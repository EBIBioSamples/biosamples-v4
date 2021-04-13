package uk.ac.ebi.biosamples.model.upload;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Characteristics {
    private static final long serialVersionUID = 1L;
    String name;
    String value;
    String iri;
    String unit;
}
