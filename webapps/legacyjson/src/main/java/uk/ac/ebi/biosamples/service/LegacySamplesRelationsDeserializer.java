package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import uk.ac.ebi.biosamples.model.LegacySamplesRelations;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.IOException;

public class LegacySamplesRelationsDeserializer extends JsonDeserializer<LegacySamplesRelations> {

    private final SampleService sampleService;

    public LegacySamplesRelationsDeserializer(SampleService sampleService) {
        this.sampleService = sampleService;
    }

    @Override
    public LegacySamplesRelations deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String accession = node.get("accession").textValue();
        Sample sample = sampleService.findByAccession(accession);
        return new LegacySamplesRelations(sample);
    }
}
