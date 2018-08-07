package uk.ac.ebi.biosamples.curation;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import uk.ac.ebi.biosamples.Application;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.service.SampleValidator;

public class NonSavingApplication extends Application {

    @Bean
    public BioSamplesClient bioSamplesClient(BioSamplesProperties bioSamplesProperties, RestTemplateBuilder restTemplateBuilder, SampleValidator sampleValidator, AapClientService aapClientService) {
        return new MockBioSamplesClient(bioSamplesProperties.getBiosamplesClientUri(), restTemplateBuilder,
                sampleValidator, aapClientService, bioSamplesProperties, true);
    }

}
