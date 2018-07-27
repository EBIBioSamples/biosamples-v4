package uk.ac.ebi.biosamples.model.phenopackets_exportation_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.ga4gh.Attributes;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghSample;
import uk.ac.ebi.biosamples.service.SampleToGa4ghSampleConverter;
import uk.ac.ebi.biosamples.service.GeoLocationDataHelper;
import uk.ac.ebi.biosamples.service.OLSDataRetriever;
import uk.ac.ebi.biosamples.service.Ga4ghSampleToPhenopacketConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Integration Test of phenopacket exporter. For test performing you need to have access to https://www.ebi.ac.uk/ols/api.
 */

public class Ga4ghSampleToPhenopacketExporterTest {
    final String biosample1Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/phenopackets_test_cases/test1/biosample1.json";
    final String biosample2Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/phenopackets_test_cases/test2/biosamples2.json";
    final String biosample3Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/phenopackets_test_cases/test3/biosample3.json";
    final String phenopacket1Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/phenopackets_test_cases/test1/phenopacket1.json";
    final String phenopacket2Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/phenopackets_test_cases/test2/phenopacket2.json";
    final String phenopacket3Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/phenopackets_test_cases/test3/phenopacket3.json";

    public SampleToGa4ghSampleConverter SampleToGa4ghSampleConverter;

    public Ga4ghSampleToPhenopacketConverter biosampleToPhenopacketExporter;
    public String absolutePath;

    public Ga4ghSampleToPhenopacketExporterTest() {
        SampleToGa4ghSampleConverter = new SampleToGa4ghSampleConverter(new Ga4ghSample(new Attributes()), new GeoLocationDataHelper());
        biosampleToPhenopacketExporter = new Ga4ghSampleToPhenopacketConverter(SampleToGa4ghSampleConverter, new OLSDataRetriever());
    }


    @Test
    public void exportation_test1() throws IOException, JSONException {

        ObjectMapper mapper = new ObjectMapper();
        Sample sample = mapper.readValue(new File(biosample1Path), Sample.class);
        Ga4ghSample ga4ghSample = SampleToGa4ghSampleConverter.convert(sample);
        String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(ga4ghSample);
        String expectedJson = new String(Files.readAllBytes(Paths.get(phenopacket1Path)));
        JSONAssert.assertEquals(expectedJson, actualJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }

    @Test
    public void exportation_test2() throws IOException, JSONException {
        ObjectMapper mapper = new ObjectMapper();
        Sample sample = mapper.readValue(new File(biosample2Path), Sample.class);
        Ga4ghSample ga4ghSample = SampleToGa4ghSampleConverter.convert(sample);
        String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(ga4ghSample);
        String expectedJson = new String(Files.readAllBytes(Paths.get(phenopacket2Path)));
        JSONAssert.assertEquals(expectedJson, actualJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }

    @Test
    public void exportation_test3() throws IOException, JSONException {
        ObjectMapper mapper = new ObjectMapper();
        Sample sample = mapper.readValue(new File(biosample3Path), Sample.class);
        Ga4ghSample ga4ghSample = SampleToGa4ghSampleConverter.convert(sample);
        String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(ga4ghSample);
        String expectedJson = new String(Files.readAllBytes(Paths.get(phenopacket3Path)));
        JSONAssert.assertEquals(expectedJson, actualJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));

    }

}