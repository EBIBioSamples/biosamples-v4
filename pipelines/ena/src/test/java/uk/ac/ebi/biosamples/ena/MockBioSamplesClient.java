package uk.ac.ebi.biosamples.ena;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleValidator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

import static org.mockito.Mockito.mock;

public class MockBioSamplesClient extends BioSamplesClient {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static PrintWriter printWriter;

    private static FileWriter fileWriter;

    private ObjectMapper objectMapper;

    private long count = 0;

    public MockBioSamplesClient(URI uri, RestTemplateBuilder restTemplateBuilder,
                                SampleValidator sampleValidator, AapClientService aapClientService,
                                BioSamplesProperties bioSamplesProperties, ObjectMapper objectMapper) {
        super(uri, restTemplateBuilder, sampleValidator, aapClientService, bioSamplesProperties);
        this.objectMapper = objectMapper;
        try {
            fileWriter = new FileWriter("export.json");
            printWriter = new PrintWriter(fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logSample(Sample sample) {
        count++;
        String sampleJson = "";
        try {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            sampleJson = objectMapper.writeValueAsString(sample);
            System.out.println(sampleJson);
        } catch (JsonProcessingException e) {

        }
        printWriter.printf("%s\n", sampleJson);
        if (count % 500 == 0) {
            log.info("Recorded " + count + " samples");
        }
    }


    @Override
    public Resource<Sample> persistSampleResource(Sample sample) {
        logSample(sample);
        return mock(Resource.class);
    }

    public void finalize() {
        try {
            fileWriter.close();
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



