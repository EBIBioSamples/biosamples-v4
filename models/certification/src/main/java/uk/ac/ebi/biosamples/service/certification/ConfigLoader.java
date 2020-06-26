package uk.ac.ebi.biosamples.service.certification;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.Config;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Service
public class ConfigLoader {
    private String configFile = "config.json";
    public Config config;

    @PostConstruct
    public void init() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Convert JSON string from file to Object
            config = mapper.readValue(this.getClass().getClassLoader().getResourceAsStream(configFile), Config.class);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
