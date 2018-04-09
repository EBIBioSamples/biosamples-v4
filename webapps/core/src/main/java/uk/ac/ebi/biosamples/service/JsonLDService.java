package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.biosamples.controller.SampleHtmlController;
import uk.ac.ebi.biosamples.model.JsonLDSample;
import uk.ac.ebi.biosamples.model.Sample;

import java.lang.reflect.Method;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

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
        JsonLDSample jsonLDSample = this.jsonLDSampleConverter.convert(sample);
        try {
            Method method = SampleHtmlController.class.getMethod("sampleAccession", String.class);
            String sampleUrl = linkTo(method, sample.getAccession()).toString();
            jsonLDSample.setUrl(sampleUrl);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return jsonLDSample;
    }


    /**
     * Convert a ld+json sample to corresponding formatted json string
     * @param jsonld the ld+json object
     * @return the formatted string representing the ld+json object
     */
    public String jsonLDToString(JsonLDSample jsonld) {

        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonld);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return this.toString();
        }
    }
}
