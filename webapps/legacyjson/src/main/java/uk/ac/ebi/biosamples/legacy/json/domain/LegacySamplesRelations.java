package uk.ac.ebi.biosamples.legacy.json.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.legacy.json.service.LegacySamplesRelationsDeserializer;

@JsonDeserialize(using = LegacySamplesRelationsDeserializer.class)
@Relation(value="samplerelations", collectionRelation = "samplesrelations")
@JsonPropertyOrder(value = {"accession", "_links"})
public class LegacySamplesRelations implements LegacyRelationship {

    private Sample sample;

    public LegacySamplesRelations(Sample sample) {
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
