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
package uk.ac.ebi.biosamples.certification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.ValidationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.service.certification.*;
import uk.ac.ebi.biosamples.validation.ElixirSchemaValidator;
import uk.ac.ebi.biosamples.validation.ValidatorI;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      Validator.class,
      ValidatorI.class,
      ElixirSchemaValidator.class,
      RestTemplate.class,
      BioSamplesProperties.class,
      ObjectMapper.class,
      Curator.class,
      Certifier.class,
      ConfigLoader.class,
      Validator.class,
      Applicator.class
    },
    properties = {"job.autorun.enabled=false"})
public class ValidatorTest {
  @Autowired
  @Qualifier("javaValidator")
  private ValidatorI validator;

  @Test
  public void given_valid_data_dont_throw_exception() throws Exception {
    final String data =
        IOUtils.toString(
            getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263.json"), "UTF8");
    validator.validate("schemas/certification/ncbi-candidate-schema.json", data);
  }

  @Test(expected = ValidationException.class)
  public void given_invalid_data_throw_exception() throws Exception {
    final String data =
        IOUtils.toString(
            getClass().getClassLoader().getResourceAsStream("json/SAMEA3774859.json"), "UTF8");
    validator.validate("schemas/certification/ncbi-candidate-schema.json", data);
  }
}
