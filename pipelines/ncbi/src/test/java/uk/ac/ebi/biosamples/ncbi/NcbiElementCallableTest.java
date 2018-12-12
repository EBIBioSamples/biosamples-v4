package uk.ac.ebi.biosamples.ncbi;

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

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class NcbiElementCallableTest {

    @MockBean
    BioSamplesClient bioSamplesClient;

    TestUtilities testUtils = new TestUtilities();
    TaxonomyService taxonService = new TaxonomyService();

    Element sample;

    @Before
    public void setup() throws Exception {
        Element sampleSet = testUtils.readNcbiBiosampleSetElementFromFile("two_organism_ncbi_sample.xml");
        sample = XmlPathBuilder.of(sampleSet).path("BioSample").element();

    }
    @Test
    public void should_read_test_document() throws Exception {
        assertThat(sample.attributeValue("accession")).isEqualTo("SAMN04192108");
    }

    @Test
    public void should_not_extract_double_organism_if_organism_is_in_description() throws Exception {
        ArgumentCaptor<Sample> generatedSample = ArgumentCaptor.forClass(Sample.class);
        when(bioSamplesClient.persistSampleResource(generatedSample.capture())).thenReturn(null);

        NcbiSampleConversionService ncbiSampleConversionService = new NcbiSampleConversionService(taxonService);
        NcbiElementCallable callable = new NcbiElementCallable(ncbiSampleConversionService, bioSamplesClient, sample, "test");
        callable.call();

        Sample sample = generatedSample.getValue();
        List<Attribute> organisms = sample.getAttributes().stream()
                .filter(attr -> attr.getType().equalsIgnoreCase("organism"))
                .collect(Collectors.toList());
        assertThat(organisms).hasSize(1);
        assertThat(organisms.get(0).getValue()).isEqualTo("Oryza sativa Japonica Group");

    }
}
