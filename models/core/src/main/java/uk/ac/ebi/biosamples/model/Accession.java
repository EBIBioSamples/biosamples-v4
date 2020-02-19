package uk.ac.ebi.biosamples.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.ac.ebi.biosamples.service.AccessionSerializer;

import java.util.Objects;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonSerialize(using = AccessionSerializer.class)
public class Accession implements Comparable<Accession> {
    protected String id;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    private Accession(String id) {
        this.id = id;
    }


    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Accession)) {
            return false;
        }
        Accession other = (Accession) o;
        return this.id.equalsIgnoreCase(other.id);
    }

    @Override
    public int compareTo(Accession other) {
        if (other == null) {
            return 1;
        }

        if (!this.id.equals(other.id)) {
            return this.id.compareTo(other.id);
        }

        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }

    @JsonCreator
    public static Accession build(@JsonProperty("id") String accession) {
        return new Accession(accession);
    }
}
