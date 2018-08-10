package uk.ac.ebi.biosamples;

import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.AMREntry;
import uk.ac.ebi.biosamples.model.structured.AMRTable;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.DataType;
import uk.ac.ebi.biosamples.ncbi.service.NcbiAmrConversionService;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;
import uk.ac.ebi.biosamples.utils.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import java.util.*;

import static org.junit.Assert.*;

public class NcbiAmrConvertionTests {

    private NcbiSampleConversionService sampleConversionService;
    private NcbiAmrConversionService amrConversionService;

    @Before
    public void setUp() {
        sampleConversionService = new NcbiSampleConversionService(new TaxonomyService());
        amrConversionService = new NcbiAmrConversionService();
    }

    @Test
    public void given_amr_data_it_produces_sample_with_structured_data() {
        Sample sampleToTest = sampleConversionService.convertNcbiXmlElementToSample(getAmrSample());
        Iterator<AbstractData> dataIterator = sampleToTest.getData().iterator();
        assertTrue("Sample should contain AMR data", dataIterator.next().getDataType().equals(DataType.AMR));
        assertFalse("Should only contain AMR data", dataIterator.hasNext());
    }

    @Test
    public void it_extract_amr_rows() {
        Sample sampleToTest = sampleConversionService.convertNcbiXmlElementToSample(getAmrSample());
        AbstractData data = sampleToTest.getData().iterator().next();
        assertTrue(data instanceof AMRTable);

        AMRTable amrTable = (AMRTable) data;
        assertTrue("Should contain exactly 1 AmrEntry but found "+ amrTable.getStructuredData().size(),
                    amrTable.getStructuredData().size() == 1);
    }

    @Test
    public void it_extract_proper_content() {
        Sample sampleToTest = sampleConversionService.convertNcbiXmlElementToSample(getAmrSample());
        AMRTable amrTable = (AMRTable) sampleToTest.getData().iterator().next();
        AMREntry amrEntry = amrTable.getStructuredData().iterator().next();

        assertEquals(amrEntry.getAntibiotic(), "nalidixic acid");
        assertEquals(amrEntry.getResistancePhenotype(), "intermediate");
        assertEquals(amrEntry.getMeasurementSign(), "==");
        assertEquals(amrEntry.getMeasurementValue(), "17");
        assertEquals(amrEntry.getMeasurementUnits(), "mm");
        assertEquals(amrEntry.getLaboratoryTypingMethod(), "disk diffusion");
        assertEquals(amrEntry.getLaboratoryTypingPlatform(), "missing");
        assertEquals(amrEntry.getLaboratoryTypingMethodVersionOrReagent(), "missing");
        assertEquals(amrEntry.getVendor(), "Becton Dickinson");
        assertEquals(amrEntry.getTestingStandard(), "CLSI");

    }

    @Test
    public void it_extract_multiple_entries_from_a_table() throws NcbiAmrConversionService.AmrParsingException {
        Element sampleWithMultipleAmrEntries = getBioSampleFromAmrSampleSet("SAMN09492289");
        Element amrTableElement = XmlPathBuilder.of(sampleWithMultipleAmrEntries).path("Description", "Comment", "Table").element();

        AMRTable amrTable = null;

        try {
            amrTable = amrConversionService.convertElementToAmrTable(amrTableElement);
        } catch (NcbiAmrConversionService.AmrParsingException e) {
            e.printStackTrace();
            throw e;
        }

        assertTrue(amrTable.getStructuredData().size() == 4);
    }

    @Test
    public void it_can_read_multiple_types_of_antibiograms_table() {
        for (Element element: getAmrSampleSet()) {
            Sample testSample = sampleConversionService.convertNcbiXmlElementToSample(element);
            AMRTable amrTable = (AMRTable) testSample.getData().first();
            assertTrue(amrTable != null);
            assertTrue(amrTable.getStructuredData().size() > 0);
        }
    }

    private Element getAmrSample() {
        return NcbiTestsService.readNcbiBiosampleElementFromFile("/examples/ncbi_amr_sample.xml");
    }

    public List<Element> getAmrSampleSet() {
        return NcbiTestsService.readNcbiBioSampleElementsFromFile("/examples/ncbi_amr_sample_set.xml");
    }

    public Element getBioSampleFromAmrSampleSet(String accession) {
        List<Element> biosamples = getAmrSampleSet();
        Optional<Element> element = biosamples.stream().filter(elem -> elem.attributeValue("accession").equals(accession)).findFirst();
        if (!element.isPresent()) {
            throw new RuntimeException("Unable to find BioSample with accession " + accession + " in the AMR sample set");
        }

        return element.get();
    }
}

