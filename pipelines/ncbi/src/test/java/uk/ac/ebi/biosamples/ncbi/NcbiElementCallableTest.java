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
package uk.ac.ebi.biosamples.ncbi;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;
import uk.ac.ebi.biosamples.utils.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class NcbiElementCallableTest {

  @MockBean(name = "AAPCLIENT")
  BioSamplesClient bioSamplesClient;

  TestUtilities testUtils = new TestUtilities();
  TaxonomyService taxonService = new TaxonomyService();

  Element sample;

  @Before
  public void setup() throws Exception {
    Element sampleSet = testUtils.readNcbiBiosampleSetFromFile("two_organism_ncbi_sample.xml");
    sample = XmlPathBuilder.of(sampleSet).path("BioSample").element();
  }

  @Test
  public void should_read_test_document() throws Exception {
    assertThat(sample.attributeValue("accession")).isEqualTo("SAMN04192108");
  }

  @Test
  public void should_extract_double_organism_if_organism_is_in_description() throws Exception {
    ArgumentCaptor<Sample> generatedSample = ArgumentCaptor.forClass(Sample.class);
    when(bioSamplesClient.persistSampleResource(generatedSample.capture())).thenReturn(null);

    NcbiSampleConversionService ncbiSampleConversionService =
        new NcbiSampleConversionService(taxonService);
    NcbiElementCallable callable =
        new NcbiElementCallable(
            ncbiSampleConversionService, bioSamplesClient, sample, "test", new HashMap<>());
    callable.call();

    Sample sample = generatedSample.getValue();
    List<Attribute> organisms =
        sample.getAttributes().stream()
            .filter(attr -> attr.getType().equalsIgnoreCase("organism"))
            .collect(Collectors.toList());
    assertThat(organisms).hasSize(2);

    assertThat(
        organisms.stream()
            .anyMatch(organism -> organism.getValue().equals("Oryza sativa Japonica Group")));
    assertThat(organisms.stream().anyMatch(organism -> organism.getValue().equals("Oryza sativa")));
  }

  @Test
  public void should_extract_double_organism_if_organism_is_in_description_with_null_amr_map()
      throws Exception {
    ArgumentCaptor<Sample> generatedSample = ArgumentCaptor.forClass(Sample.class);
    when(bioSamplesClient.persistSampleResource(generatedSample.capture())).thenReturn(null);

    NcbiSampleConversionService ncbiSampleConversionService =
        new NcbiSampleConversionService(taxonService);
    NcbiElementCallable callable =
        new NcbiElementCallable(
            ncbiSampleConversionService, bioSamplesClient, sample, "test", null);
    callable.call();

    Sample sample = generatedSample.getValue();
    List<Attribute> organisms =
        sample.getAttributes().stream()
            .filter(attr -> attr.getType().equalsIgnoreCase("organism"))
            .collect(Collectors.toList());
    assertThat(organisms).hasSize(2);

    assertThat(
        organisms.stream()
            .anyMatch(organism -> organism.getValue().equals("Oryza sativa Japonica Group")));
    assertThat(organisms.stream().anyMatch(organism -> organism.getValue().equals("Oryza sativa")));
  }
}
