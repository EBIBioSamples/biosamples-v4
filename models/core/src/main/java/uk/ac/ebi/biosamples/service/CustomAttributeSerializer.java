package uk.ac.ebi.biosamples.service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

public class CustomAttributeSerializer extends StdSerializer<SortedSet> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public CustomAttributeSerializer(){
		this(SortedSet.class);
	}

	public CustomAttributeSerializer(Class<SortedSet> t) {
		super(t);
	}

	@Override
	public void serialize(SortedSet attributesRaw, JsonGenerator gen, SerializerProvider arg2)
			throws IOException, JsonProcessingException {

		
		SortedSet<Attribute> attributes = (SortedSet<Attribute>)attributesRaw;
		
		gen.writeStartObject();
		SortedMap<String, SortedMap<String,Attribute>> attributeMap = new TreeMap<>();
		if (attributes != null && attributes.size() > 0) {
			for (Attribute attribute : attributes) {
				if (!attributeMap.containsKey(attribute.getType())) {
					attributeMap.put(attribute.getType(), new TreeMap<>());
				}
				attributeMap.get(attribute.getType()).put(attribute.getValue(), Attribute.build(null, null, attribute.getIri(), attribute.getUnit()));			
			}

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
		}

        gen.writeEndObject();
	}
}
