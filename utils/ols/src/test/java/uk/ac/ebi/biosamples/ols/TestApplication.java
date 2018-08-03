package uk.ac.ebi.biosamples.ols;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;

@Configuration
@ComponentScan
public class TestApplication {
/*
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
*/
    @Bean
    public BioSamplesProperties bioSamplesProperties() {
        return new BioSamplesProperties();
    }
}
