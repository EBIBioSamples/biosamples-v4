package uk.ac.ebi.biosamples.legacy.json.domain;

import org.springframework.hateoas.core.Relation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import uk.ac.ebi.biosamples.legacy.json.service.SampleRelationsDeserializer;
import uk.ac.ebi.biosamples.model.Sample;

@JsonDeserialize(using = SampleRelationsDeserializer.class)
@Relation(value="samplerelations", collectionRelation = "samplesrelations")
@JsonPropertyOrder(value = {"accession", "_links"})
public class SamplesRelations implements Relations {

    private Sample sample;

    public SamplesRelations(Sample sample) {
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
