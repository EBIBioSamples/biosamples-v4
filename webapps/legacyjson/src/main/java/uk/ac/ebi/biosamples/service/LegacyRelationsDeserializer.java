package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import uk.ac.ebi.biosamples.model.LegacyRelations;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.IOException;

public class LegacyRelationsDeserializer extends JsonDeserializer<LegacyRelations> {

    private final SampleService sampleService;

    public LegacyRelationsDeserializer(SampleService sampleService) {
        this.sampleService = sampleService;
    }

    @Override
    public LegacyRelations deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String accession = node.get("accession").textValue();
        Sample sample = sampleService.findByAccession(accession);
        return new LegacyRelations(sample);
    }
}
