package uk.ac.ebi.biosamples;

import org.dom4j.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ncbi.NcbiSampleConversionService;
import uk.ac.ebi.biosamples.utils.TaxonomyService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;

@RunWith(SpringRunner.class)
public class NcbiBaseConverterTests {

    private NcbiSampleConversionService conversionService;

    private Element testNcbiBioSamples;

    @Before
    public void setup() {
        this.conversionService = new NcbiSampleConversionService(new TaxonomyService());
        try {
            this.testNcbiBioSamples = readBioSampleElementFromXml("/examples/ncbi_sample_5246317.xml");
        } catch (DocumentException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void given_ncbi_biosample_extract_accession_and_alias() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        assert(sampleToTest.getAccession().equals("SAMN05246317"));
        assert(sampleToTest.getName().equals("GF.26.AL.R"));
    }

    @Test
    public void given_ncbi_biosamples_it_generates_and_insdc_secondary_accession_attribute() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter(attr -> attr.getType().equals("INSDC secondary accession")).findFirst();
        assert(expectedAttribute.isPresent());

        Attribute secondaryAccession = expectedAttribute.get();
        assert(secondaryAccession.getValue().equals("SRS1524325"));
    }

    @Test
    public void it_extracts_insdc_center_name() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter(attr -> attr.getType().equals("INSDC center name")).findFirst();
        assert(expectedAttribute.isPresent());

        Attribute centerName = expectedAttribute.get();
        assert(centerName.getValue().equals("Lund University"));

    }

    public Element readBioSampleElementFromXml(String pathToFile) throws DocumentException {
        InputStream xmlInputStream = this.getClass().getResourceAsStream(pathToFile);
        String xmlDocument = new BufferedReader(new InputStreamReader(xmlInputStream)).lines().collect(Collectors.joining());
        Document doc = DocumentHelper.parseText(xmlDocument);
        return doc.getRootElement().element("BioSample");
    }

}
