package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.JsonLDDataRecord;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.IOException;

public class SampleToJsonLDSampleRecordConverterTest {
    private static final Logger log = LoggerFactory.getLogger(SampleToJsonLDSampleRecordConverterTest.class);

    @Test
    public void testConvert() {
        Sample sample = getSample();

        SampleToJsonLDSampleRecordConverter converter = new SampleToJsonLDSampleRecordConverter();
        JsonLDDataRecord record = converter.convert(sample);

        Assert.assertEquals(sample.getAttributes().first().getIri().first(),
                record.getMainEntity().getAdditionalProperties().get(0).getValueReference().get(0).getId());
    }

    @Test
    public void testSerializeDeserialize() {
        Sample sample = getSample();
        SampleToJsonLDSampleRecordConverter converter = new SampleToJsonLDSampleRecordConverter();
        JsonLDDataRecord record = converter.convert(sample);

        JsonLDDataRecord deserializedRecord = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            String serializedRecord = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(record);
            deserializedRecord = mapper.readValue(serializedRecord, JsonLDDataRecord.class);
        } catch (IOException e) {
            log.error("Failed to serialize JsonLD record");
            Assert.fail();
        }

        Assert.assertEquals(record.getContext().getOtherContexts().size(), deserializedRecord.getContext().getOtherContexts().size());
    }

    private Sample getSample() {
        return new Sample.Builder("FakeName", "FakeAccession")
                .withDomain("test.fake.domain")
                .addAttribute(Attribute.build("Organism", "Homo Sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null))
                .build();
    }
}
