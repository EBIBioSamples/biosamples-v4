package uk.ac.ebi.biosamples.ena;

import org.dom4j.DocumentException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.ena.EnaXmlEnhancer.*;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static uk.ac.ebi.biosamples.ena.EnaXmlUtil.pretty;
import static uk.ac.ebi.biosamples.ena.ExampleSamples.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, properties = {"job.autorun.enabled=false"})
public class EnaXmlEnhancerTest {

    @Autowired
    private EnaElementConverter enaElementConverter;

    @Autowired
    private EnaXmlEnhancer enaXmlEnhancer;

    private EnaDatabaseSample enaDatabaseSample;

    @Before
    public void setup() {
        enaDatabaseSample = enaXmlEnhancer.getEnaDatabaseSample("SRS000121");
        /*
        enaDatabaseSample.firstPublic = "2018-01-01";
        enaDatabaseSample.lastUpdated = "2018-02-01";
        enaDatabaseSample.brokerName = "broker";
        enaDatabaseSample.bioSamplesId = "SAMN00001603";
        enaDatabaseSample.centreName = "expanded center name";
        */
    }

    @Test
    public void test_xml_with_all_rules() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedFullSampleXml), enaXmlEnhancer.applyAllRules(fullSampleXml, enaDatabaseSample));
    }

    @Test
    public void test_center_name_rule_fixes_applicable_ebi_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedCenterNameSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, CenterNameRule.INSTANCE));
    }

    @Test
    public void test_biosamples_rule_fixes_applicable_ebi_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedEbiBiosamplesSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, BioSamplesIdRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_fixes_applicable_ebi_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedEbiBrokerSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, BrokerRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_fixes_applicable_ncbi_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedNcbiBrokerSampleXml), enaXmlEnhancer.applyRules(ncbiSampleXml, enaDatabaseSample, BrokerRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_fixes_applicable_ddbj_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedDdbjBrokerSampleXml), enaXmlEnhancer.applyRules(ddbjSampleXml, enaDatabaseSample, BrokerRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_does_not_change_non_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaXmlEnhancer.getEnaDatabaseSample(""), BrokerRule.INSTANCE));
    }

    @Test
    public void test_alias_rule_fixes_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedMissingAliasSampleXml), enaXmlEnhancer.applyRules(missingAliasSampleXml, enaDatabaseSample, AliasRule.INSTANCE));
    }

    @Test
    public void test_alias_rule_does_not_change_non_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, AliasRule.INSTANCE));
    }

    @Test
    public void test_namespace_rule_fixes_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(missingNamespaceSampleXml, enaDatabaseSample, NamespaceRule.INSTANCE));
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(emptyNamespaceSampleXml, enaDatabaseSample, NamespaceRule.INSTANCE));
    }

    @Test
    public void test_namespace_rule_does_not_change_non_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, NamespaceRule.INSTANCE));
    }

    @Test
    @Ignore
    public void test_link_removal_rule_fixes_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedNcbiLinksRemoved), enaXmlEnhancer.applyRules(ncbiSampleXml, enaDatabaseSample, LinkRemovalRule.INSTANCE));
    }

    @Test
    public void test_first_public_and_last_updated_for_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXmlWithDates), enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, DatesRule.INSTANCE));
    }

}
