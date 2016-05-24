package uk.ac.ebi.biosamples.models;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


public class SampleSerializer extends JsonSerializer<Sample> {

	@Override
	public void serialize(Sample sample, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
        gen.writeStartObject();
        gen.writeStringField("accession", sample.getAccession());
        gen.writeStringField("name", sample.getName());
        gen.writeArrayFieldStart("attributes");
		for (String key : sample.getAttributeTypes()) {
			for (String value : sample.getAttributeValues(key)) {
				gen.writeStartObject();
				gen.writeStringField("key", key);
				gen.writeStringField("value", value);
				if (sample.getAttributeUnit(key, value).isPresent()) {
					gen.writeStringField("unit", sample.getAttributeUnit(key, value).get());
				}
				if (sample.getAttributeOntologyTerm(key, value).isPresent()) {
					gen.writeStringField("ontologyTerm", sample.getAttributeOntologyTerm(key, value).get().toString());
				}
				gen.writeEndObject();				
			}
		}
        gen.writeEndArray();
        gen.writeEndObject();
		
	}

}
