package uk.ac.ebi.biosamples.certification;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.certification.Config;
import uk.ac.ebi.biosamples.service.certification.ConfigLoader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConfigLoader.class, properties = {"job.autorun.enabled=false"})
public class ConfigLoaderTest {
    @Autowired
    private ConfigLoader configLoader;

    @Test
    public void return_a_valid_config() {
        Config config = configLoader.config;
        assertNotNull(config);
        assertFalse(config.getChecklists().isEmpty());
    }
}
