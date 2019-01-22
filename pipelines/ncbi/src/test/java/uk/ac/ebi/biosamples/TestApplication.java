package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.ncbi.MockBioSamplesClient;
import uk.ac.ebi.biosamples.service.SampleValidator;

@Configuration
@EnableAutoConfiguration
public class TestApplication extends Application {

    @Autowired
    @Bean
    public BioSamplesClient bioSamplesClient(BioSamplesProperties bioSamplesProperties, RestTemplateBuilder restTemplateBuilder, SampleValidator sampleValidator, AapClientService aapClientService, ObjectMapper objectMapper) {
        return new MockBioSamplesClient(bioSamplesProperties.getBiosamplesClientUri(), restTemplateBuilder,
                sampleValidator, aapClientService, bioSamplesProperties, objectMapper);
    }

}
