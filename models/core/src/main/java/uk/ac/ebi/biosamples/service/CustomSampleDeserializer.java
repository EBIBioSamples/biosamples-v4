package uk.ac.ebi.biosamples.service;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

public class CustomSampleDeserializer extends StdDeserializer<Sample> {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	public CustomSampleDeserializer() {
		this(Sample.class);
	}

	public CustomSampleDeserializer(Class<Sample> t) {
		super(t);
	}

	@Override
	public Sample deserialize(JsonParser jsonParser, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        
        String accession = null;
        if (jsonNode.has("accession") ) {
        	accession = jsonNode.get("accession").asText();
        }
        String name = null;
        if (jsonNode.has("name")) {
        	name = jsonNode.get("name").asText();
        }
        LocalDateTime update = LocalDateTime.parse(jsonNode.get("update").asText());
        LocalDateTime release = LocalDateTime.parse(jsonNode.get("release").asText());
        
        SortedSet<Attribute> attributes = new TreeSet<>();
        if (jsonNode.has("characteristics")) {
        	//log.info("characteristics node type is "+jsonNode.get("characteristics").getNodeType());
	        Iterator<Entry<String, JsonNode>> itKey = jsonNode.get("characteristics").fields();
	        while (itKey.hasNext()) {
	        	Entry<String, JsonNode> entryKey = itKey.next();
	        	String key = entryKey.getKey();
	        	//log.info("characteristics key "+key);
	        	//log.info("characteristics key "+key+" node type is "+ entryKey.getValue().getNodeType());
	            Iterator<Entry<String, JsonNode>> itValue = entryKey.getValue().fields();
	            while (itValue.hasNext()) {
	            	Entry<String, JsonNode> entryValue = itValue.next();
	    			String value = entryValue.getKey();
		        	//log.info("characteristics key "+key+" value "+value);
	    			
	    			String unit = null;
	    			if (entryValue.getValue().get("unit") != null) {
	    				unit = entryValue.getValue().get("unit").asText();
	    			}
	    			
	    			URI iri = null;
	    			if (entryValue.getValue().get("iri") != null) {
	    				String iriString = entryValue.getValue().get("iri").asText();
	    				try {
	    					iri = URI.create(iriString);
	    				} catch (IllegalArgumentException e) {
	    					ctxt.handleWeirdStringValue(URI.class, iriString, "Unable to read iri as valid URI");
	    				}
	    			}
	    			Attribute attribute = Attribute.build(key, value, iri, unit);
	    			//log.info("adding attribute "+attribute);
	    			attributes.add(attribute);
	            }
	        }
        }
        
        
        SortedSet<Relationship> relationships = new TreeSet<>();
        if (jsonNode.has("relationships")) {
	        Iterator<JsonNode> itRelationships = jsonNode.get("relationships").elements();
	        while (itRelationships.hasNext()) {
	        	JsonNode relationship = itRelationships.next();
	        	String source = relationship.get("source").asText();
	        	String type = relationship.get("type").asText();
	        	String target = relationship.get("target").asText();
	        	relationships.add(Relationship.build(type, target, source));
	        }        
        }
                
        return Sample.build(name, accession, release, update, attributes, relationships);
        
	}
}
