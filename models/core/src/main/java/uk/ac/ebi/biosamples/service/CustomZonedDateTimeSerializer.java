package uk.ac.ebi.biosamples.service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CustomZonedDateTimeSerializer extends StdSerializer<ZonedDateTime> {

	public CustomZonedDateTimeSerializer() {
		this(null);
	}

	public CustomZonedDateTimeSerializer(Class<ZonedDateTime> t) {
		super(t);
	}

	@Override
	public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider arg2)
			throws IOException, JsonProcessingException {
		gen.writeString(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(value));
	}
}