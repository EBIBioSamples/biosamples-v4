package uk.ac.ebi.biosamples.solr;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.solr.model.field.*;
import uk.ac.ebi.biosamples.solr.service.SolrFieldService;
import uk.ac.ebi.biosamples.solr.service.SolrFilterService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SolrSampleAccessionField.class, SolrSampleAttributeValueField.class, SolrSampleDataTypeField.class,
SolrSampleDateField.class, SolrSampleDomainField.class, SolrSampleExternalReferenceDataField.class,
SolrSampleInverseRelationField.class, SolrSampleNameField.class, SolrSampleRelationField.class,
SolrFilterService.class, SolrFieldService.class, BioSamplesProperties.class})
public class SolrServiceTests {


    @Autowired
    SolrFilterService solrFilterService;

    @Autowired
    private SolrFieldService fieldService;


//    @Before
//    public void setup() {
//        this.fieldService = new SolrFieldService(solrSampleFieldList);
//    }

    @Test
    public void given_encoded_sample_decode_it_correctly_and_of_the_right_type() {
        String encodedField = "MRSXGY3SNFYHI2LPNY_______av_ss";
        String expectedDecodedField = "description";

        SolrSampleField sampleField = fieldService.decodeField(encodedField);
        assertEquals(sampleField.getReadableLabel(), expectedDecodedField);
        assertTrue(sampleField instanceof SolrSampleAttributeValueField);

    }

    @Test
    public void given_fields_with_similar_suffix_return_the_correct_type() {

        SolrSampleField dataTypeField = fieldService.decodeField("structdatatype_ss");
        SolrSampleField attributeField = fieldService.decodeField("MRSXGY3SNFYHI2LPNY_______av_ss");
        SolrSampleField nameField = fieldService.decodeField("name_s");
        SolrSampleField domainField = fieldService.decodeField("domain_s");

        assertTrue(dataTypeField instanceof SolrSampleDataTypeField);
        assertEquals(dataTypeField.getReadableLabel(), "structdatatype");
        assertEquals(dataTypeField.getSolrLabel(), "structdatatype_ss");

        assertTrue(attributeField instanceof SolrSampleAttributeValueField);
        assertTrue(nameField instanceof SolrSampleNameField);
        assertTrue(domainField instanceof SolrSampleDomainField);

    }


    @Test
    public void given_filter_object_return_the_corresponding_solr_field() {
        Filter organismFilter = FilterBuilder.create()
                        .onAttribute("organism")
                        .withValue("Homo sapiens")
                        .build();
        SolrSampleAttributeValueField organism = new SolrSampleAttributeValueField("organism");
        Optional<Criteria> criteriaFromField = Optional.ofNullable(organism.getFilterCriteria(organismFilter));
        Optional<Criteria> criteriaFromService = solrFilterService.getFilterCriteria(organismFilter);

        assertEquals(criteriaFromField.toString(), criteriaFromService.toString());
    }


}
