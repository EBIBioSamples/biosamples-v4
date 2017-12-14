package uk.ac.ebi.biosamples.utils;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 
 * Utility class that reads an input stream of JSON and calls a
 * provided handler for each element of interest. The handler is given a DOM
 * populated element to do something with
 *
 *
 */
@Service
public class JsonFragmenter {

	private JsonFragmenter() {}
	
	public void handleStream(InputStream inputStream, String encoding, JsonCallback callback)
			throws Exception {

		ObjectMapper mapper = new ObjectMapper();
	    JsonParser parser = mapper.getFactory().createParser(inputStream);
	    if (parser.nextToken() != JsonToken.START_ARRAY) {
	    	throw new IllegalStateException("A JSON array was expected");
		}
		while(parser.nextToken() == JsonToken.START_OBJECT) {
			JsonNode sampleNode = mapper.readTree(parser);
            if (sampleNode.has("accession")) {
                String biosampleSerialization = mapper.writeValueAsString(sampleNode);
                callback.handleJson(biosampleSerialization);
            }
		}

	}

	public interface JsonCallback {
		/**
		 * This function is passed a DOM element of interest for further processing.
		 * 
		 * @param json
		 * @throws Exception 
		 */
		public void handleJson(String json) throws Exception;

	}
}
