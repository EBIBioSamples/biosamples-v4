package uk.ac.ebi.biosamples.solr;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.solr.model.field.*;
import uk.ac.ebi.biosamples.solr.service.SolrFieldService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SolrSampleAccessionField.class, SolrSampleAttributeValueField.class, SolrSampleDataTypeField.class,
SolrSampleDateField.class, SolrSampleDomainField.class, SolrSampleExternalReferenceDataField.class,
SolrSampleInverseRelationField.class, SolrSampleNameField.class, SolrSampleRelationField.class})
public class SolrFieldTests {


    @Autowired
    List<SolrSampleField> solrSampleFieldList;

    private SolrFieldService fieldService;

    @Before
    public void setup() {
        this.fieldService = new SolrFieldService(solrSampleFieldList);
    }

    @Test
    public void given_encoded_sample_decode_it_correctly_and_of_the_right_type() {
        String encodedField = "MRSXGY3SNFYHI2LPNY_______av_ss";
        String expectedDecodedField = "description";

        SolrSampleField sampleField = fieldService.decodeField(encodedField);
        assertEquals(sampleField.getReadableLabel(), expectedDecodedField);
        assertTrue(sampleField instanceof SolrSampleAttributeValueField);

    }


}
