/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.springframework.stereotype.Service;

/**
 * Utility class that reads an input stream of JSON and calls a provided handler for each element of
 * interest. The handler is given a DOM populated element to do something with
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
    while (parser.nextToken() == JsonToken.START_OBJECT) {
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
