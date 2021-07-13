/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service.certification;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.SampleDocument;

@Service
public class Identifier {
  private static Logger EVENTS = LoggerFactory.getLogger("events");

  public SampleDocument identify(String data) {
    if (data == null) {
      throw new IllegalArgumentException("cannot identify a null data");
    }

    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    try {
      SampleDocument sampleDocument = mapper.readValue(data, SampleDocument.class);
      String accession = sampleDocument.getAccession();
      String message = "";

      if (accession != null && !accession.isEmpty()) message = accession;
      else message = "New sample";

      sampleDocument.setDocument(data);
      EVENTS.info(String.format("%s identification successful", message));

      return sampleDocument;
    } catch (IOException e) {
      String uuid = UUID.randomUUID().toString();
      EVENTS.info(String.format("%s identification failed for sample, assigned UUID", uuid));
      return new SampleDocument(uuid, data);
    }
  }
}
