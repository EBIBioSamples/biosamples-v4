package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.JsonLDSample;
import uk.ac.ebi.biosamples.model.Sample;

/**
 * This servise is meant for the convertions jobs to/form ld+json
 */
@Service
public class JsonLDService {

    ObjectMapper objectMapper;

    SampleToJsonLDSampleConverter jsonLDSampleConverter;

    public JsonLDService(ObjectMapper mapper) {
        this.jsonLDSampleConverter = new SampleToJsonLDSampleConverter();
        this.objectMapper = mapper;
    }

    /**
     * Produce the ld+json version of a sample
     * @param sample the sample to convert
     * @return the ld+json version of the sample
     */
    public JsonLDSample sampleToJsonLD(Sample sample) {
        return this.jsonLDSampleConverter.convert(sample);
    }


    /**
     * Convert a ld+json sample to corresponding formatted json string
     * @param jsonld the ld+json object
     * @return the formatted string representing the ld+json object
     */
    public String jsonLDToString(JsonLDSample jsonld) {
        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return this.toString();
        }
    }
}
