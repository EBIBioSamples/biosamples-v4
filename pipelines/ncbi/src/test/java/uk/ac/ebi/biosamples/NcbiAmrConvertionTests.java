package uk.ac.ebi.biosamples;

import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.hamcrest.*;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.AMREntry;
import uk.ac.ebi.biosamples.model.structured.AMRTable;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.DataType;
import uk.ac.ebi.biosamples.ncbi.NcbiSampleConversionService;
import uk.ac.ebi.biosamples.utils.TaxonomyService;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

import static org.junit.Assert.*;

public class NcbiAmrConvertionTests {

    private NcbiSampleConversionService sampleConversionService;

    @Before
    public void setUp() {
        sampleConversionService = new NcbiSampleConversionService(new TaxonomyService());
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

    private Element getAmrSample() {
        return NcbiTestsService.readNcbiBiosampleElementFromFile("/examples/ncbi_amr_sample.xml");
    }
}
