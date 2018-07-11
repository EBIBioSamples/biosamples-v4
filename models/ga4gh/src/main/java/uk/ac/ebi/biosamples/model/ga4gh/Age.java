package uk.ac.ebi.biosamples.model.ga4gh;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class Age {
    private String age;
    private OntologyTerm age_class;

    @JsonProperty("age")
    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    @JsonProperty("age_class")
    public OntologyTerm getAge_class() {
        return age_class;
    }

    public void setAge_class(OntologyTerm age_class) {
        this.age_class = age_class;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Age age1 = (Age) o;
        return Objects.equals(age, age1.age) &&
                Objects.equals(age_class, age1.age_class);
    }

    @Override
    public int hashCode() {

        return Objects.hash(age, age_class);
    }
}
