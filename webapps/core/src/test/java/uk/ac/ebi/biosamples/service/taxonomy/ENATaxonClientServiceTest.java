package uk.ac.ebi.biosamples.service.taxonomy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.File;
import java.io.IOException;

public class ENATaxonClientServiceTest {
    @Test
    public void validateSample() throws IOException {
        File file = new File(getClass().getClassLoader().getResource("json/ncbi-SAMN03894263-curated.json").getFile());
        ObjectMapper objectMapper = new ObjectMapper();
        ENATaxonClientService enaTaxonClientService = new ENATaxonClientService();
        Sample sample = objectMapper.readValue(file, Sample.class);

        sample = enaTaxonClientService.performTaxonomyValidation(sample);

        final String organismInSample = sample
                .getAttributes()
                .stream()
                .filter(attr -> attr.getType().equalsIgnoreCase("Organism"))
                .findFirst()
                .get()
                .getValue();

        Assert.assertTrue(organismInSample != null);
    }
}

