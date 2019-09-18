package uk.ac.ebi.biosamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableCaching
public class Application {

    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(Application.class, args));
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateCustomizer restTemplateCustomizer) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplateCustomizer.customize(restTemplate);
        return restTemplate;
    }

    @Bean
    public RestTemplateCustomizer restTemplateCustomizer(BioSamplesProperties bioSamplesProperties, PipelinesProperties pipelinesProperties) {
        return new PipelinesHelper().getRestTemplateCustomizer(bioSamplesProperties, pipelinesProperties);
    }
}