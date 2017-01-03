package uk.ac.ebi.biosamples.models;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CustomZonedDateTimeDeserializer extends StdDeserializer<ZonedDateTime> {

	public CustomZonedDateTimeDeserializer() {
		this(null);
	}

	public CustomZonedDateTimeDeserializer(Class<ZonedDateTime> t) {
		super(t);
	}

	@Override
	public ZonedDateTime deserialize(JsonParser jsonparser, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		String date = jsonparser.getText();
		try {
			return ZonedDateTime.parse(date);
		} catch (DateTimeParseException e) {
			throw new RuntimeException(e);
		}
	}
}