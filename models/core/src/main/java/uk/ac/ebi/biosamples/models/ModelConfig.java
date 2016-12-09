package uk.ac.ebi.biosamples.models;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class ModelConfig {
	
	/*
	 * For some reason, this doens't seem to work. so this has been replaced with a sample annotation and custom classes
	 */
	@Bean
	public Jackson2ObjectMapperBuilderCustomizer getLocalDateTimeJackson2ObjectMapperBuilderCustomizer() {
		return new Jackson2ObjectMapperBuilderCustomizer(){
			@Override
			public void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {
				jacksonObjectMapperBuilder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>(){
					@Override
					public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
							throws IOException, JsonProcessingException {
						gen.writeString(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value));						
					}});
				jacksonObjectMapperBuilder.deserializerByType(LocalDateTime.class, new JsonDeserializer<LocalDateTime>(){
					@Override
					public LocalDateTime deserialize(JsonParser jsonparser, DeserializationContext ctxt)
							throws IOException, JsonProcessingException {
						String date = jsonparser.getText();
				        try {
				            return LocalDateTime.parse(date);
				        } catch (DateTimeParseException e) {
				            throw new RuntimeException(e);
				        }
					}});
			}};
	}
	    
	@Bean
	public Module getJavaTimeModule() {
		return new JavaTimeModule();
	}
}
