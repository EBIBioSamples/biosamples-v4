package uk.ac.ebi.biosamples.legacy.json.service;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleRelationsDeserializer extends JsonDeserializer<SamplesRelations> {

    private final SampleRepository sampleRepository;

    public SampleRelationsDeserializer(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    @Override
    public SamplesRelations deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String accession = node.get("accession").textValue();
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        return sample.map(SamplesRelations::new).orElse(null);
    }
}
