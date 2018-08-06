package uk.ac.ebi.biosamples.curation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.ols.OlsProcessor;
import uk.ac.ebi.biosamples.service.CurationApplicationService;

import java.net.URISyntaxException;
import java.util.Arrays;

@Configuration
public class TestApplication {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jackson2HalModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
        halConverter.setObjectMapper(mapper);
        halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
        return mapper;
    }

    @Bean
    public BioSamplesProperties bioSamplesProperties() {
        return new BioSamplesProperties();
    }

    @Bean
    public OlsProcessor olsProcessor() {
        return new OlsProcessor(restTemplate(), bioSamplesProperties());
    }

    @Bean
    public CurationApplicationService curationApplicationService() {
        return new CurationApplicationService();
    }


    @Bean
    public MockBioSamplesClient mockBioSamplesClient() throws URISyntaxException {
        return new MockBioSamplesClient(bioSamplesProperties());
    }
}
