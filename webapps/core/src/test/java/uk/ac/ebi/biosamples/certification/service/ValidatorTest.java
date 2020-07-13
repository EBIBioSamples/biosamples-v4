package uk.ac.ebi.biosamples.certification.service;

import org.apache.commons.io.IOUtils;
import org.everit.json.schema.ValidationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.service.certification.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Validator.class, Curator.class, Certifier.class, ConfigLoader.class, Validator.class, Applicator.class},  properties = {"job.autorun.enabled=false"})
public class ValidatorTest {
    @Autowired
    private Validator validator;

    @Test
    public void given_valid_data_dont_throw_exception() throws Exception {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263.json"), "UTF8");
        validator.validate("schemas/certification/ncbi-candidate-schema.json", data);
    }

    @Test(expected = ValidationException.class)
    public void given_invalid_data_throw_exception() throws Exception {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/SAMEA3774859.json"), "UTF8");
        validator.validate("schemas/certification/ncbi-candidate-schema.json", data);
    }

}
