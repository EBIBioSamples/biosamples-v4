package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import uk.ac.ebi.biosamples.model.LegacySamplesRelations;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.IOException;
import java.util.Optional;

public class LegacySamplesRelationsDeserializer extends JsonDeserializer<LegacySamplesRelations> {

    private final SampleRepository sampleRepository;

    public LegacySamplesRelationsDeserializer(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    @Override
    public LegacySamplesRelations deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String accession = node.get("accession").textValue();
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        return sample.map(LegacySamplesRelations::new).orElse(null);
    }
}
