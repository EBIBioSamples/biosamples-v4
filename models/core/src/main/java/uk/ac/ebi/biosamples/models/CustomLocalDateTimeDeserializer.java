package uk.ac.ebi.biosamples.models;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CustomLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

	public CustomLocalDateTimeDeserializer() {
		this(null);
	}

	public CustomLocalDateTimeDeserializer(Class<LocalDateTime> t) {
		super(t);
	}

	@Override
	public LocalDateTime deserialize(JsonParser jsonparser, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		String date = jsonparser.getText();
		try {
			return LocalDateTime.parse(date);
		} catch (DateTimeParseException e) {
			throw new RuntimeException(e);
		}
	}
}