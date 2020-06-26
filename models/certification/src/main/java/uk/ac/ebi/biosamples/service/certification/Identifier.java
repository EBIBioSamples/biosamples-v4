package uk.ac.ebi.biosamples.service.certification;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.SampleDocument;

import java.io.IOException;
import java.util.UUID;

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
            sampleDocument.setDocument(data);
            EVENTS.info(String.format("%s identification successful", sampleDocument.getAccession()));
            return sampleDocument;
        } catch (IOException e) {
            String uuid = UUID.randomUUID().toString();
            EVENTS.info(String.format("%s identification failed for sample, assigned UUID", uuid));
            return new SampleDocument(uuid, data);
        }
    }
}
