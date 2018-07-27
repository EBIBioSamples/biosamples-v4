
package uk.ac.ebi.biosamples;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghSample;
import uk.ac.ebi.biosamples.service.SampleToGa4ghSampleConverter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = Application.class)
@TestPropertySource(
        locations = "classpath:test.properties")
public class SampleToGa4ghSampleConverterTest {
    @Autowired
    SampleToGa4ghSampleConverter mapper;
    @Autowired
    BioSamplesClient client;

    //covers biocharacteristics, attributes, externalidentifiers
    @Test
    public void real_sample_test1() throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        String sampleJson = "SAMEA1367515";
        String biosampleJson = readFile("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/ga4gh_tests/ga4gh_test_jsons/GA4GHSAMEA1367515.json", StandardCharsets.UTF_8);
        Sample sample = client.fetchSampleResource(sampleJson).get().getContent();
        Ga4ghSample biosample = mapper.convert(sample);
        String mappedBiosampleJson = jsonMapper.writeValueAsString(biosample);
        try {
            JSONAssert.assertEquals(biosampleJson, mappedBiosampleJson, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Test
    public void real_sample_deserialization_test1() throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        String sampleJson = "SAMEA1367515";
        String biosampleJson = readFile("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/ga4gh_tests/ga4gh_test_jsons/GA4GHSAMEA1367515.json", StandardCharsets.UTF_8);
        Sample sample = client.fetchSampleResource(sampleJson).get().getContent();
        Ga4ghSample biosample = mapper.convert(sample);
        String mappedBiosampleJson = jsonMapper.writeValueAsString(biosample);//jsonMapper.writeValueAsString(biosample)
        Ga4ghSample deserialized_biosample = jsonMapper.readValue(biosampleJson, Ga4ghSample.class);
        Assert.assertTrue(biosample.equals(deserialized_biosample));
    }

    //covers biocharacteristics, attributes
    @Test
    public void real_sample_mapping_test2() throws IOException {
        JsonParser parser = new JsonParser();
        ObjectMapper jsonMapper = new ObjectMapper();
        String sampleJson = "SAMN07666496";
        String biosampleJson = readFile("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/ga4gh_tests/ga4gh_test_jsons/GA4GHSAMN07666496.json", StandardCharsets.UTF_8);
        Sample sample = client.fetchSampleResource(sampleJson).get().getContent();
        Ga4ghSample biosample = mapper.convert(sample);
        String mappedBiosampleJson = jsonMapper.writeValueAsString(biosample);
        try {
            JSONAssert.assertEquals(biosampleJson, mappedBiosampleJson, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void real_sample_deserialization_test2() throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        String sampleJson = "SAMN07666496";
        String biosampleJson = readFile("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/ga4gh_tests/ga4gh_test_jsons/GA4GHSAMN07666496.json", StandardCharsets.UTF_8);
        Sample sample = client.fetchSampleResource(sampleJson).get().getContent();
        Ga4ghSample biosample = mapper.convert(sample);
        Ga4ghSample deserialized_biosample = jsonMapper.readValue(biosampleJson, Ga4ghSample.class);
        Assert.assertTrue(biosample.equals(deserialized_biosample));
    }

    //age, location, biochracteristics, attributes
    @Test
    public void real_sample_mapping_test3() throws IOException {
        JsonParser parser = new JsonParser();
        ObjectMapper jsonMapper = new ObjectMapper();
        String sampleJson = "SAMEA2672955";
        String biosampleJson = readFile("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/ga4gh_tests/ga4gh_test_jsons/GA4GHSAMEA2672955.json", StandardCharsets.UTF_8);
        Sample sample = client.fetchSampleResource(sampleJson).get().getContent();
        Ga4ghSample biosample = mapper.convert(sample);
        String mappedBiosampleJson = jsonMapper.writeValueAsString(biosample);
        try {
            JSONAssert.assertEquals(biosampleJson, mappedBiosampleJson, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void real_sample_deserialization_test3() throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        String sampleJson = "SAMEA2672955";
        String biosampleJson = readFile("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/ga4gh_tests/ga4gh_test_jsons/GA4GHSAMEA2672955.json", StandardCharsets.UTF_8);
        Sample sample = client.fetchSampleResource(sampleJson).get().getContent();
        Ga4ghSample biosample = mapper.convert(sample);
        Ga4ghSample deserialized_biosample = jsonMapper.readValue(biosampleJson, Ga4ghSample.class);
        Assert.assertTrue(biosample.equals(deserialized_biosample));
    }

    //covers age, attributes, biocharacteristics
    @Test
    public void real_sample_test4() throws IOException {
        JsonParser parser = new JsonParser();
        ObjectMapper jsonMapper = new ObjectMapper();
        String sampleJson = "SAMN07566236";
        String biosampleJson = readFile("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/ga4gh_tests/ga4gh_test_jsons/GA4GHSAMN07566236.json", StandardCharsets.UTF_8);
        Sample sample = client.fetchSampleResource(sampleJson).get().getContent();
        Ga4ghSample biosample = mapper.convert(sample);
        String mappedBiosampleJson = jsonMapper.writeValueAsString(biosample);
        try {
            JSONAssert.assertEquals(biosampleJson, mappedBiosampleJson, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void real_sample_deserialization_test4() throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        String sampleJson = "SAMN07566236";
        String biosampleJson = readFile("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/ga4gh_tests/ga4gh_test_jsons/GA4GHSAMN07566236.json", StandardCharsets.UTF_8);
        Sample sample = client.fetchSampleResource(sampleJson).get().getContent();
        Ga4ghSample biosample = mapper.convert(sample);
        Ga4ghSample deserialized_biosample = jsonMapper.readValue(biosampleJson, Ga4ghSample.class);
        Assert.assertTrue(biosample.equals(deserialized_biosample));
    }

    //covers external identifiers, biocharcteristics, attributes
    @Test
    public void real_sample_test5() throws IOException {
        JsonParser parser = new JsonParser();
        ObjectMapper jsonMapper = new ObjectMapper();
        String sampleJson = "SAMEA281881";
        String biosampleJson = readFile("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/ga4gh_tests/ga4gh_test_jsons/GA4GHSAMEA3121488.json", StandardCharsets.UTF_8);
        Sample sample = client.fetchSampleResource(sampleJson).get().getContent();
        Ga4ghSample biosample = mapper.convert(sample);
        String mappedBiosampleJson = jsonMapper.writeValueAsString(biosample);
        try {
            JSONAssert.assertEquals(biosampleJson, mappedBiosampleJson, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void real_sample_deserialization_test5() throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        String sampleJson = "SAMEA281881";
        String biosampleJson = readFile("/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/ga4gh/src/test/java/uk/ac/ebi/biosamples/model/ga4gh_tests/ga4gh_test_jsons/GA4GHSAMEA3121488.json", StandardCharsets.UTF_8);
        Sample sample = client.fetchSampleResource(sampleJson).get().getContent();
        Ga4ghSample biosample = mapper.convert(sample);
        Ga4ghSample deserialized_biosample = jsonMapper.readValue(biosampleJson, Ga4ghSample.class);
        Assert.assertTrue(biosample.equals(deserialized_biosample));
    }


    static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}