package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.JsonLDSample;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class ConvertionService {

    SampleToJsonLDSampleConverter jsonLDSampleConverter;

    public ConvertionService() {
        this.jsonLDSampleConverter = new SampleToJsonLDSampleConverter();
    }

    public JsonLDSample sampleToJsonLD(Sample sample) {
        return this.jsonLDSampleConverter.convert(sample);
    }
}
