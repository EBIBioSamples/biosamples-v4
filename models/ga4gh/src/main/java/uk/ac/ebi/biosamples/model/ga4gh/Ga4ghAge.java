package uk.ac.ebi.biosamples.model.ga4gh;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class Ga4ghAge {
    private String age;
    private Ga4ghOntologyTerm age_class;

    @JsonProperty("age")
    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    @JsonProperty("age_class")
    public Ga4ghOntologyTerm getAge_class() {
        return age_class;
    }

    public void setAge_class(Ga4ghOntologyTerm age_class) {
        this.age_class = age_class;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ga4ghAge age1 = (Ga4ghAge) o;
        return Objects.equals(age, age1.age) &&
                Objects.equals(age_class, age1.age_class);
    }

    @Override
    public int hashCode() {

        return Objects.hash(age, age_class);
    }

    @Override
    public Ga4ghAge clone() {
        try {
            return (Ga4ghAge) super.clone();
        }catch (CloneNotSupportedException e){
            return new Ga4ghAge();
        }

    }
}
