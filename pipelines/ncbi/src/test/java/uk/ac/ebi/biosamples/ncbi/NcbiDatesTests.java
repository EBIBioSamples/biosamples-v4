package uk.ac.ebi.biosamples.ncbi;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.NcbiTestsService;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;
import uk.ac.ebi.biosamples.utils.TaxonomyService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class NcbiDatesTests {

    private NcbiSampleConversionService conversionService;

    private Element testNcbiBioSamples;

    @Before
    public void setup() {
        this.conversionService = new NcbiSampleConversionService(new TaxonomyService());
        this.testNcbiBioSamples = NcbiTestsService.readNcbiBiosampleElementFromFile("/examples/biosample_result_test.xml");
    }

    @Test
    public void given_ncbi_biosamples_it_generates_and_insdc_first_public_attribute() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples, new HashSet<>());
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter(attr -> attr.getType().equals("INSDC first public")).findFirst();
        Attribute secondaryAccession = expectedAttribute.get();

        assertEquals(secondaryAccession.getValue(), "2019-05-30T00:00:00Z");
    }

    @Test
    public void it_extracts_create() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples, new HashSet<>());

        assertTrue(sampleToTest.getCreate() != null);
        assertEquals(sampleToTest.getCreate().toString(), "2019-05-30T14:12:04.443Z");
        System.out.println(sampleToTest);
    }

    @Test
    public void it_extracts_insdc_dates() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples, new HashSet<>());
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream().filter(attr -> attr.getType().equals("INSDC first public"))
                .findFirst();

        assertTrue(expectedAttribute.isPresent());

        Attribute insdcFirstPublic = expectedAttribute.get();

        assertEquals(insdcFirstPublic.getValue(), "2019-05-30T00:00:00Z");

        expectedAttribute = sampleToTest.getAttributes().stream().filter(attr -> attr.getType().equals("INSDC last update")).findFirst();

        assertTrue(expectedAttribute.isPresent());

        Attribute insdcLastUpdate = expectedAttribute.get();

        assertEquals(insdcLastUpdate.getValue(), "2019-11-29T13:12:34.104Z");

    }

    public Element readBioSampleElementFromXml(String pathToFile) throws DocumentException {
        InputStream xmlInputStream = this.getClass().getResourceAsStream(pathToFile);
        String xmlDocument = new BufferedReader(new InputStreamReader(xmlInputStream)).lines().collect(Collectors.joining());
        Document doc = DocumentHelper.parseText(xmlDocument);

        return doc.getRootElement().element("BioSample");
    }

}
