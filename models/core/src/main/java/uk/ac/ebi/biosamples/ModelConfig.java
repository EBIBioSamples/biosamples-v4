package uk.ac.ebi.biosamples;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfig {

//    @Bean
//    @Order(1)
//    public Jackson2ObjectMapperBuilderCustomizer customizer() {
//        return (Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) -> {
//            Map<Class<?>, JsonSerializer<?>> serializers = new HashMap<>();
//            Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<>();
//
//            serializers.put(Sample.class, new CustomSampleSerializer());
//            serializers.put(Instant.class, new CustomInstantSerializer());
//            deserializers.put(Sample.class, new CustomSampleDeserializer());
//            deserializers.put(Sample.class, new CustomSampleDeserializer());
//
//            jackson2ObjectMapperBuilder.serializersByType(serializers);
//            jackson2ObjectMapperBuilder.deserializersByType(deserializers);
//        };
//    }
}
