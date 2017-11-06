package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.LegacySampleSerializer;

@Configuration
public class LegacyJsonConfiguration {

    Logger log = LoggerFactory.getLogger(getClass());

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customize() {
        return new MapperCustomizer();
    }

    private static class MapperCustomizer implements Jackson2ObjectMapperBuilderCustomizer, Ordered {
        Logger log = LoggerFactory.getLogger(getClass());
        @Override
        public void customize(Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
            log.info("Passing the customize method");
            jackson2ObjectMapperBuilder.serializerByType(Sample.class, new LegacySampleSerializer());
        }
        @Override
        public int getOrder() {
            return -1;
        }
    }


}
