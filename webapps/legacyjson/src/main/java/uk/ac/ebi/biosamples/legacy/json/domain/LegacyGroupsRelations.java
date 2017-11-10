package uk.ac.ebi.biosamples.legacy.json.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.legacy.json.service.LegacySamplesRelationsDeserializer;

@JsonDeserialize(using = LegacySamplesRelationsDeserializer.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
@Relation(value = "grouprelations", collectionRelation = "groupsrelations")
public class LegacyGroupsRelations implements LegacyRelationship {

    private Sample sample;

    public LegacyGroupsRelations(Sample sample) {
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
