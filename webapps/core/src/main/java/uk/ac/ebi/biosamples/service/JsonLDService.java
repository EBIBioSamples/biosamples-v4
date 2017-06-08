package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.JsonLDSample;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class JsonLDService {

    ObjectMapper objectMapper;

    SampleToJsonLDSampleConverter jsonLDSampleConverter;

    public JsonLDService(ObjectMapper mapper) {
        this.jsonLDSampleConverter = new SampleToJsonLDSampleConverter();
        this.objectMapper = mapper;
    }

    public JsonLDSample sampleToJsonLD(Sample sample) {
        return this.jsonLDSampleConverter.convert(sample);
    }

    public String jsonLDToString(JsonLDSample jsonLD) {
        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonLD);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return this.toString();
        }
    }
}
