package uk.ac.ebi.biosamples.solr;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CustomInstantSolrDeserializer extends StdDeserializer<Instant> {

		
	public CustomInstantSolrDeserializer() {
		this(null);
	}

	public CustomInstantSolrDeserializer(Class<Instant> t) {
		super(t);
	}
	
	@Override
	public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return Instant.parse(p.readValueAs(String.class));
	}
}