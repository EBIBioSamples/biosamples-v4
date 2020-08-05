package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.DataType;
import uk.ac.ebi.biosamples.model.structured.HistologyEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredTable;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;
import uk.ac.ebi.biosamples.utils.TestUtilities;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

@Component
public class StructuredDataIntegration extends AbstractIntegration {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private ObjectMapper mapper;

    public StructuredDataIntegration(BioSamplesClient client) {
        super(client);
        this.mapper = new ObjectMapper();
    }

    @Override
    protected void phaseOne() {
        String json = TestUtilities.readFileAsString("histology_sample.json");
        Sample sample;
        try {
            sample = mapper.readValue(json, Sample.class);
        } catch (IOException e) {
            throw new IntegrationTestFailException("An error occurred while converting json to Sample class" + e, Phase.ONE);
        }

        Resource<Sample> submittedSample = this.client.persistSampleResource(sample);
        if (!sample.equals(submittedSample.getContent())) {
            throw new IntegrationTestFailException("Expected: " + sample + ", found: " + submittedSample.getContent(), Phase.ONE);
        }
    }

    @Override
    protected void phaseTwo() {
        Optional<Resource<Sample>> sampleResource = client.fetchSampleResource("StructuredDataIntegration_sample_1");
        if (sampleResource.isEmpty()) {
            throw new IntegrationTestFailException("Expected structured data sample not present", Phase.TWO);
        }

        Sample sample = sampleResource.get().getContent();
        log.info("Checking sample has histology data");
        assertEquals(sample.getData().size(), 1);
        assertEquals(sample.getData().first().getDataType(), DataType.HISTOLOGY);

        StructuredTable<HistologyEntry> structuredTable = (StructuredTable<HistologyEntry>) sample.getData().first();

        log.info("Check structured data table has correct number of entries");
        assertEquals(structuredTable.getStructuredData().size(), 1);

        log.info("Verifying structured data content");
        Optional<HistologyEntry> optionalEntry = structuredTable.getStructuredData().parallelStream()
                .filter(entry -> entry.getMarker().getValue().equalsIgnoreCase("Cortisol"))
                .findFirst();
        if (optionalEntry.isEmpty()) {
            throw new IntegrationTestFailException("Structured data content verification failed", Phase.TWO);
        }

        HistologyEntry entry = optionalEntry.get();
        assertEquals(entry.getMarker().getValue(), "Cortisol");
        assertEquals(entry.getMeasurement().getValue(), "0.000");
        assertEquals(entry.getMeasurementUnits().getValue(), "log pg/mg feather");
        assertEquals(entry.getPartner().getValue(), "IRTA");
    }

    @Override
    protected void phaseThree() {
        //skip
    }

    @Override
    protected void phaseFour() {
        //skip
    }

    @Override
    protected void phaseFive() {
        //skip
    }

}
