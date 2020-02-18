package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;
import uk.ac.ebi.biosamples.model.structured.DataType;
import uk.ac.ebi.biosamples.utils.TestUtilities;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Component
public class AmrDataIntegration extends AbstractIntegration {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private final RestTemplate restTemplate;
    private BioSamplesProperties clientProperties;
    private ObjectMapper mapper;

    public AmrDataIntegration(BioSamplesClient client, RestTemplateBuilder restTemplateBuilder, BioSamplesProperties clientProperties) {
        super(client);
        this.restTemplate = restTemplateBuilder.build();
        this.mapper = new ObjectMapper();
        this.clientProperties = clientProperties;
    }

    @Override
    protected void phaseOne() {
        String json = TestUtilities.readFileAsString("amr_sample.json");
        Sample amrSample = null;
        try {
            amrSample = mapper.readValue(json, Sample.class);
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while converting json to Sample class", e);
        }

        Resource<Sample> submittedSample = this.client.persistSampleResource(amrSample);
        if (!amrSample.equals(submittedSample.getContent())) {
            log.warn("expected: "+amrSample);
            log.warn("found: "+submittedSample.getContent());
            throw new RuntimeException("Expected response to equal submission");
        }

    }

    @Override
    protected void phaseTwo() {

        Optional<Resource<Sample>> sampleResource = client.fetchSampleResource("TestAMR");
        if (!sampleResource.isPresent()) {
            throw new RuntimeException("Sample TestAMR should be available at this stage");
        }

        Sample amrSample = sampleResource.get().getContent();
        log.info("Checking sample has amr data");
        assertEquals(amrSample.getData().size(), 1);
        assertEquals(amrSample.getData().first().getDataType(), DataType.AMR);

        AMRTable amrTable = (AMRTable) amrSample.getData().first();

        // Assert there are 15 entries
        log.info("Check amr table has the right amount of entries");
        assertEquals(amrTable.getStructuredData().size(), 15);

        // Assert there are only 2 entries with missing testing standard
        assertEquals(amrTable.getStructuredData().parallelStream()
                .filter(entry -> entry.getAstStandard().equalsIgnoreCase("missing"))
                .collect(Collectors.toList()).size(), 2);

        log.info("Verifying AMREntry for ciprofloxacin is found and has certain values");
        Optional<AMREntry> optionalAmrEntry = amrTable.getStructuredData().parallelStream()
                .filter(entry -> entry.getAntibiotic().getValue().equalsIgnoreCase("ciprofloxacin"))
                .findFirst();
        if (!optionalAmrEntry.isPresent()) {
            throw new RuntimeException("AMRentry for antibiotic ciprofloxacin should be present but is not");
        }

        AMREntry ciprofloxacin = optionalAmrEntry.get();
        assertEquals(ciprofloxacin.getResistancePhenotype(), "susceptible");
        assertEquals(ciprofloxacin.getMeasurementSign(), "<=");
        assertEquals(ciprofloxacin.getMeasurement(), "0.015");
        assertEquals(ciprofloxacin.getMeasurementUnits(), "mg/L");
        assertEquals(ciprofloxacin.getLaboratoryTypingMethod(), "MIC");
        assertEquals(ciprofloxacin.getPlatform(), "");
        assertEquals(ciprofloxacin.getLaboratoryTypingMethodVersionOrReagent(), "96-Well Plate");
        assertEquals(ciprofloxacin.getVendor(), "Trek");
        assertEquals(ciprofloxacin.getAstStandard(), "CLSI");

    }

    @Override
    protected void phaseThree() {

    }

    @Override
    protected void phaseFour() {

    }

    @Override
    protected void phaseFive() {

    }

//    public String readFileAsString(String resource){
//
//        String json;
//
//        try {
//            json =StreamUtils.copyToString(new ClassPathResource(resource).getInputStream(), Charset.defaultCharset());
//        } catch (IOException e) {
//            throw new RuntimeException("An error occurred while reading resource " + resource, e);
//        }
//
//        return json;
//
//    }
//
//    public JsonNode readAsJsonObject(String serialisedJson) {
//        try {
//            return mapper.readTree(serialisedJson);
//        } catch (IOException e) {
//            throw new RuntimeException("An error occurred while converting the string " + serialisedJson + " to a JSON object", e);
//        }
//    }

}
