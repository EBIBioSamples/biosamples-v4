package uk.ac.ebi.biosamples;

import org.dom4j.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.xml.sax.SAXException;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ncbi.NcbiElementCallableFactory;
import uk.ac.ebi.biosamples.ncbi.NcbiFragmentCallback;
import uk.ac.ebi.biosamples.ncbi.SampleConversionService;
import uk.ac.ebi.biosamples.utils.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlFragmenter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class NCBIConverterTests {

    private SampleConversionService conversionService;

    private Element testNcbiBioSamples;

    @Before
    public void setup() {
        this.conversionService = new SampleConversionService(new TaxonomyService());
        try {
            this.testNcbiBioSamples = readBioSampleElementFromXml("/examples/ncbi_sample_5246317.xml");
        } catch (DocumentException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void given_ncbi_biosample_extract_accession_and_alias() {
        Sample.Builder sampleBuilder = this.conversionService.sampleBuilderFromElement(this.testNcbiBioSamples);
        Sample sampleToTest = sampleBuilder.build();
        assert(sampleToTest.getAccession().equals("SAMN05246317"));
        assert(sampleToTest.getName().equals("GF.26.AL.R"));

    }

    public Element readBioSampleElementFromXml(String pathToFile) throws DocumentException {
        InputStream xmlInputStream = this.getClass().getResourceAsStream(pathToFile);
        String xmlDocument = new BufferedReader(new InputStreamReader(xmlInputStream)).lines().collect(Collectors.joining());
        Document doc = DocumentHelper.parseText(xmlDocument);
        return doc.getRootElement().element("BioSample");
    }

}
