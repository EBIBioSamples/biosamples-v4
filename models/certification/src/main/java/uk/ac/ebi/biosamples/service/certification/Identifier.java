package uk.ac.ebi.biosamples.service.certification;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.Sample;

import java.io.IOException;
import java.util.UUID;

@Service
public class Identifier {

    private static Logger EVENTS = LoggerFactory.getLogger("events");

    public Sample identify(String data) {
        if (data == null) {
            throw new IllegalArgumentException("cannot identify a null data");
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        try {
            Sample sample = mapper.readValue(data, Sample.class);
            sample.setDocument(data);
            EVENTS.info(String.format("%s identification successful", sample.getAccession()));
            return sample;
        } catch (IOException e) {
            String uuid = UUID.randomUUID().toString();
            EVENTS.info(String.format("%s identification failed for sample, assigned UUID", uuid));
            return new Sample(uuid, data);
        }
    }
}
