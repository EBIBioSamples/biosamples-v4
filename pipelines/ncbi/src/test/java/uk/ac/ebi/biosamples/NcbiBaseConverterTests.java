package uk.ac.ebi.biosamples;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

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
    public void given_ncbi_biosample_extract_accession_name_synonym() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        assertEquals(sampleToTest.getAccession(), "SAMN05246317");
        assertEquals(sampleToTest.getName(), "GF.26.AL.R");
    }

    @Test
    public void given_ncbi_biosamples_it_generates_and_insdc_secondary_accession_attribute() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter(attr -> attr.getType().equals("INSDC secondary accession")).findFirst();

        Attribute secondaryAccession = expectedAttribute.get();
        assertEquals(secondaryAccession.getValue(),"SRS1524325");
    }

    @Test
    public void it_extracts_insdc_center_name() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter(attr -> attr.getType().equals("INSDC center name")).findFirst();
        assertTrue(expectedAttribute.isPresent());

        Attribute centerName = expectedAttribute.get();
        assertEquals(centerName.getValue(), "Lund University");
    }

    @Test
    public void it_extracts_organism_attribute() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter( attr -> attr.getType().equals("organism"))
                .findFirst();

        assertTrue(expectedAttribute.isPresent());

        Attribute organism = expectedAttribute.get();
        assertEquals(organism.getValue(), "soil metagenome");
        assertEquals(organism.getIri().first(), "http://purl.obolibrary.org/obo/NCBITaxon_410658");
    }

    @Test
    public void it_extracts_description_title() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter( attr -> attr.getType().equals("description title"))
                .findFirst();

        assertTrue(expectedAttribute.isPresent());

        Attribute description = expectedAttribute.get();
        assertEquals(description.getValue(), "Metagenome or environmental sample from soil metagenome");

    }

    @Test
    public void it_extracts_attributes() {
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        SortedSet<Attribute> sampleAttributes = sampleToTest.getAttributes();
        List<Attribute> expectedAttributes = Stream.of(
                Attribute.build("isolation source", "Alseis blackiana roots"),
                Attribute.build("collection date", "Sep-2012"),
                Attribute.build("geographic location", "Panama:Gigante_peninsula"),
                Attribute.build("latitude and longitude", "9.110057 N 79.8434 W"),
                Attribute.build("Fert_treat", "unfertilized"),
                Attribute.build("plot", "GF_26")
        ).collect(Collectors.toList());


        Optional<Attribute> attributesNotMatching = expectedAttributes.stream()
                .filter( attr -> !sampleAttributes.contains(attr)).findAny();
        assertTrue(!attributesNotMatching.isPresent());

    }

    @Test
    public void it_extracts_ncbi_submission_model(){
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter( attr -> attr.getType().equals("NCBI submission model"))
                .findFirst();

        assertTrue(expectedAttribute.isPresent());

        Attribute ncbiSubmissionModel = expectedAttribute.get();
        assertEquals(ncbiSubmissionModel.getValue(), "Metagenome or environmental");

    }

    @Test
    public void it_extracts_ncbi_submission_package(){
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter( attr -> attr.getType().equals("NCBI submission package"))
                .findFirst();

        assertTrue(expectedAttribute.isPresent());

        Attribute ncbiSubmissionPackage = expectedAttribute.get();
        assertEquals(ncbiSubmissionPackage.getValue(), "Metagenome.environmental.2.0");

    }

    @Test
    public void it_extracts_insdc_dates(){
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter( attr -> attr.getType().equals("INSDC first public"))
                .findFirst();

        assertTrue(expectedAttribute.isPresent());

        Attribute insdcFirstPublic = expectedAttribute.get();
        assertEquals(insdcFirstPublic.getValue(), "2018-07-01T00:50:05.513Z");

        expectedAttribute = sampleToTest.getAttributes().stream()
                .filter( attr -> attr.getType().equals("INSDC last update"))
                .findFirst();

        assertTrue(expectedAttribute.isPresent());

        Attribute insdcLastUpdate = expectedAttribute.get();
        assertEquals(insdcLastUpdate.getValue(), "2018-07-01T00:50:05.513Z");


    }

    @Test
    public void it_extracts_insdc_status(){
        Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(this.testNcbiBioSamples);
        Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter( attr -> attr.getType().equals("INSDC status"))
                .findFirst();

        assertTrue(expectedAttribute.isPresent());

        Attribute insdcStatus = expectedAttribute.get();
        assertEquals(insdcStatus.getValue(), "live");

    }

    @Test
    public void given_ncbi_status_not_live_it_set_release_date_in_the_future() throws DocumentException {
            Element ncbiSampleNotLive = readBioSampleElementFromXml("/examples/ncbi_test_sample_not_live.xml");
            Sample sampleToTest = this.conversionService.convertNcbiXmlElementToSample(ncbiSampleNotLive);
            Optional<Attribute> expectedAttribute = sampleToTest.getAttributes().stream()
                .filter( attr -> attr.getType().equals("INSDC status"))
                .findFirst();

            assertTrue(expectedAttribute.isPresent());
            assertNotEquals(expectedAttribute.get().getValue(), "live");

            // Sample release date is set in the future
            assertTrue(sampleToTest.getRelease().isAfter(Instant.parse("3018-07-01T00:50:05.00Z")));


    }

    public Element readBioSampleElementFromXml(String pathToFile) throws DocumentException {
        InputStream xmlInputStream = this.getClass().getResourceAsStream(pathToFile);
        String xmlDocument = new BufferedReader(new InputStreamReader(xmlInputStream)).lines().collect(Collectors.joining());
        Document doc = DocumentHelper.parseText(xmlDocument);
        return doc.getRootElement().element("BioSample");
    }

}
