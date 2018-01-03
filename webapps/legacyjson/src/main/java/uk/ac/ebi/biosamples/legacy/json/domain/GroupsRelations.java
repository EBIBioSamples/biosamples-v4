package uk.ac.ebi.biosamples.legacy.json.domain;

import org.springframework.hateoas.core.Relation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import uk.ac.ebi.biosamples.legacy.json.service.SampleRelationsDeserializer;
import uk.ac.ebi.biosamples.model.Sample;

@JsonDeserialize(using = SampleRelationsDeserializer.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
@Relation(value = "grouprelations", collectionRelation = "groupsrelations")
public class GroupsRelations implements Relations {

    private Sample sample;

    public GroupsRelations(Sample sample) {
        this.sample = sample;
    }

    @JsonProperty
    public String accession() {
        return this.sample.getAccession();
    }

    @JsonIgnore
    public Sample getAssociatedSample() {
        return this.sample;
    }
}
