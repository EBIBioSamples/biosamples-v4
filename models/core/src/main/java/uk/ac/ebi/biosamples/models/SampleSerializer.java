package uk.ac.ebi.biosamples.models;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

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

		gen.writeStringField("update", sample.getUpdate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		gen.writeStringField("release", sample.getRelease().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

		if (sample.getAttributeKeys() != null && sample.getAttributeKeys().size() > 0) {
			gen.writeArrayFieldStart("attributes");
			for (String key : sample.getAttributeKeys()) {
				for (String value : sample.getAttributeValues(key)) {
					gen.writeStartObject();
					gen.writeStringField("key", key);
					gen.writeStringField("value", value);
					if (sample.getAttributeUnit(key, value) != null) {
						gen.writeStringField("unit", sample.getAttributeUnit(key, value));
					}
					if (sample.getAttributeOntologyTerm(key, value) != null) {
						gen.writeStringField("ontologyTerm", sample.getAttributeOntologyTerm(key, value).toString());
					}
					gen.writeEndObject();
				}
			}
			gen.writeEndArray();
		}

		if (sample.getRelationshipTypes() != null && sample.getRelationshipTypes().size() > 0) {
			gen.writeArrayFieldStart("relationships");
			for (String type : sample.getRelationshipTypes()) {
				for (String target : sample.getRelationshipTargets(type)) {
					gen.writeStartObject();
					gen.writeStringField("type", type);
					gen.writeStringField("target", target);
					gen.writeEndObject();
				}
			}
			gen.writeEndArray();
		}

		gen.writeEndObject();

	}

}
