package uk.ac.ebi.biosamples.service.certification;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class Validator {
    private Logger log = LoggerFactory.getLogger(getClass());

    public void validate(String schemaPath, String document) throws IOException, ValidationException {
        log.info("Schema path is " + schemaPath);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(schemaPath)) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(new JSONObject(document));
        }
    }
}
