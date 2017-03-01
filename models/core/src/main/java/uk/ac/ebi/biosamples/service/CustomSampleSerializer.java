package uk.ac.ebi.biosamples.service;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.SortedMap;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

public class CustomSampleSerializer extends StdSerializer<Sample> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public CustomSampleSerializer() {
		this(Sample.class);
	}

	public CustomSampleSerializer(Class<Sample> t) {
		super(t);
	}

	@Override
	public void serialize(Sample sample, JsonGenerator gen, SerializerProvider arg2)
			throws IOException, JsonProcessingException {
		gen.writeStartObject();
		if (sample.getAccession() != null) {
			gen.writeStringField("accession", sample.getAccession());
		}
		gen.writeStringField("name", sample.getName());
        gen.writeStringField("update", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(sample.getUpdate()));
        gen.writeStringField("release", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(sample.getRelease()));
        
		SortedMap<String, SortedMap<String,Attribute>> attributeMap = new TreeMap<>();
		if (sample.getAttributes() != null && sample.getAttributes().size() > 0) {
			for (Attribute attribute : sample.getAttributes()) {
				if (!attributeMap.containsKey(attribute.getKey())) {
					attributeMap.put(attribute.getKey(), new TreeMap<>());
				}
				attributeMap.get(attribute.getKey()).put(attribute.getValue(), Attribute.build(null, null, attribute.getIri(), attribute.getUnit()));			
			}

	        gen.writeObjectFieldStart("characteristics");
	        for (String type : attributeMap.keySet()) {
	        	gen.writeObjectFieldStart(type);
	            for (String value : attributeMap.get(type).keySet()) {
	            	gen.writeObjectFieldStart(value);
	            	if (attributeMap.get(type).get(value).getIri() != null) {
	            		gen.writeStringField("iri", attributeMap.get(type).get(value).getIri().toString());
	            	}
	            	if (attributeMap.get(type).get(value).getUnit() != null) {
	            		gen.writeStringField("unit", attributeMap.get(type).get(value).getUnit());		
	            	}
	                gen.writeEndObject();
	            }
	            gen.writeEndObject();
	        }
	        gen.writeEndObject();   
		}
             

		if (sample.getRelationships() != null && sample.getRelationships().size() > 0) {
	        gen.writeObjectField("relationships", sample.getRelationships());
		}

		if (sample.getExternalReferences() != null && sample.getExternalReferences().size() > 0) {
	        gen.writeObjectField("externalReferences", sample.getExternalReferences());
		}

        gen.writeEndObject();
	}
}
