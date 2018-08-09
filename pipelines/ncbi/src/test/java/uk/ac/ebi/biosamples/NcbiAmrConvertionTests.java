package uk.ac.ebi.biosamples;

import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.hamcrest.*;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ncbi.NcbiSampleConversionService;
import uk.ac.ebi.biosamples.utils.TaxonomyService;

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
        assertTrue("Sample should contain AMR data", !sampleToTest.getData().isEmpty());

    }

    private Element getAmrSample() {
        return NcbiTestsService.readNcbiBiosampleElementFromFile("/examples/ncbi_amr_sample.xml");
    }
}
