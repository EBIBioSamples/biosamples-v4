package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.core.io.ClassPathResource;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghAttributes;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghSample;
import uk.ac.ebi.biosamples.service.SampleToGa4ghSampleConverter;
import uk.ac.ebi.biosamples.service.GeoLocationDataHelper;
import uk.ac.ebi.biosamples.service.OLSDataRetriever;
import uk.ac.ebi.biosamples.service.Ga4ghSampleToPhenopacketConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Integration Test of phenopacket exporter. For test performing you need to have access to https://www.ebi.ac.uk/ols/api.
 */
@Ignore
public class Ga4ghSampleToPhenopacketExporterTest {
    final String biosample1Path = "/biosample1.json";
    final String biosample2Path = "/biosamples2.json";
    final String biosample3Path = "/biosample3.json";
    final String phenopacket1Path = "/phenopacket1.json";
    final String phenopacket2Path = "/phenopacket2.json";
    final String phenopacket3Path = "/phenopacket3.json";

    public SampleToGa4ghSampleConverter SampleToGa4ghSampleConverter;

    public Ga4ghSampleToPhenopacketConverter biosampleToPhenopacketExporter;
    public String absolutePath;

    public Ga4ghSampleToPhenopacketExporterTest() {
        SampleToGa4ghSampleConverter = new SampleToGa4ghSampleConverter(new Ga4ghSample(new Ga4ghAttributes()), new GeoLocationDataHelper());
        biosampleToPhenopacketExporter = new Ga4ghSampleToPhenopacketConverter(SampleToGa4ghSampleConverter, new OLSDataRetriever());
    }


    @Test
    public void exportation_test1() throws IOException, JSONException {

        ObjectMapper mapper = new ObjectMapper();
        Sample sample = mapper.readValue(getJsonString(biosample1Path), Sample.class);
        Ga4ghSample ga4ghSample = SampleToGa4ghSampleConverter.convert(sample);
        String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(ga4ghSample);
        String expectedJson = getJsonString(phenopacket1Path);
        JSONAssert.assertEquals(expectedJson, actualJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }

    @Test
    public void exportation_test2() throws IOException, JSONException {
        ObjectMapper mapper = new ObjectMapper();
        Sample sample = mapper.readValue(getJsonString(biosample2Path), Sample.class);
        Ga4ghSample ga4ghSample = SampleToGa4ghSampleConverter.convert(sample);
        String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(ga4ghSample);
        String expectedJson = getJsonString(phenopacket2Path);
        JSONAssert.assertEquals(expectedJson, actualJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }

    @Test
    public void exportation_test3() throws IOException, JSONException {
        ObjectMapper mapper = new ObjectMapper();
        Sample sample = mapper.readValue(getJsonString(biosample3Path), Sample.class);
        Ga4ghSample ga4ghSample = SampleToGa4ghSampleConverter.convert(sample);
        String actualJson = biosampleToPhenopacketExporter.getJsonFormattedPhenopacket(ga4ghSample);
        String expectedJson = getJsonString(phenopacket3Path);
        JSONAssert.assertEquals(expectedJson, actualJson, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));

    }


    static String getJsonString(String path) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new ClassPathResource(path).getInputStream()), 4096);
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            stringBuilder.append(line).append('\n');
        }
        br.close();
        String expectedJson = stringBuilder.toString();
        return expectedJson;
    }
}