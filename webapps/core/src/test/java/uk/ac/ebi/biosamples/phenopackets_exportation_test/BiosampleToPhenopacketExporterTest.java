package uk.ac.ebi.biosamples.phenopackets_exportation_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.ga4gh_model.Attributes;
import uk.ac.ebi.biosamples.model.ga4gh_model.Biosample;
import uk.ac.ebi.biosamples.service.ga4ghService.BiosampleToGA4GHMapper;
import uk.ac.ebi.biosamples.service.ga4ghService.GeoLocationDataHelper;
import uk.ac.ebi.biosamples.service.ga4ghService.OLSDataRetriever;
import uk.ac.ebi.biosamples.service.phenopackets_exportation_service.BiosampleToPhenopacketExporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class BiosampleToPhenopacketExporterTest {
    final String biosample1Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/webapps/core/src/test/java/uk/ac/ebi/biosamples/phenopackets_test_cases/test1/biosample1.json";
    final String biosample2Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/webapps/core/src/test/java/uk/ac/ebi/biosamples/phenopackets_test_cases/test2/biosamples2.json";
    final String biosample3Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/webapps/core/src/test/java/uk/ac/ebi/biosamples/phenopackets_test_cases/test3/biosample3.json";
    final String phenopacket1Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/webapps/core/src/test/java/uk/ac/ebi/biosamples/phenopackets_test_cases/test1/phenopacket1.json";
    final String phenopacket2Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/webapps/core/src/test/java/uk/ac/ebi/biosamples/phenopackets_test_cases/test2/phenopacket2.json";
    final String phenopacket3Path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/webapps/core/src/test/java/uk/ac/ebi/biosamples/phenopackets_test_cases/test3/phenopacket3.json";

    public BiosampleToGA4GHMapper biosampleToGA4GHMapper;

    public BiosampleToPhenopacketExporter biosampleToPhenopacketExporter;

    public BiosampleToPhenopacketExporterTest() {
        biosampleToGA4GHMapper = new BiosampleToGA4GHMapper(new Biosample(new Attributes()), new GeoLocationDataHelper());
        biosampleToPhenopacketExporter = new BiosampleToPhenopacketExporter(null, biosampleToGA4GHMapper, new OLSDataRetriever());
    }


    @Test
    public void exportation_test1() throws IOException, JSONException {
        ObjectMapper mapper = new ObjectMapper();
        Sample sample = mapper.readValue(new File(biosample1Path), Sample.class);
        Biosample biosample = biosampleToGA4GHMapper.mapSampleToGA4GH(sample);
        String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(biosample);
        String expectedJson = new String(Files.readAllBytes(Paths.get(phenopacket1Path)));
        JSONAssert.assertEquals(expectedJson, actualJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }

    @Test
    public void exportation_test2() throws IOException, JSONException {
        ObjectMapper mapper = new ObjectMapper();
        Sample sample = mapper.readValue(new File(biosample2Path), Sample.class);
        Biosample biosample = biosampleToGA4GHMapper.mapSampleToGA4GH(sample);
        String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(biosample);
        String expectedJson = new String(Files.readAllBytes(Paths.get(phenopacket2Path)));
        JSONAssert.assertEquals(expectedJson, actualJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }

    @Test
    public void exportation_test3() throws IOException, JSONException {
        ObjectMapper mapper = new ObjectMapper();
        Sample sample = mapper.readValue(new File(biosample3Path), Sample.class);
        Biosample biosample = biosampleToGA4GHMapper.mapSampleToGA4GH(sample);
        String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(biosample);
        String expectedJson = new String(Files.readAllBytes(Paths.get(phenopacket3Path)));
        JSONAssert.assertEquals(expectedJson, actualJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));

    }

}