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

    private DatabaseSample databaseSample;

    @Before
    public void setup() {
        databaseSample = enaXmlEnhancer.getDatabaseSample();
        databaseSample.firstPublic = "2018-01-01";
        databaseSample.lastUpdated = "2018-02-01";
        databaseSample.brokerName = "broker";
        databaseSample.bioSamplesId = "SAMN00001603";
        databaseSample.centreName = "expanded center name";
    }

    @Test
    public void test_xml_with_all_rules() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(fullSampleXml), enaXmlEnhancer.applyAllRules(fullSampleXml, databaseSample));
    }

    @Test
    public void test_center_name_rule_fixes_applicable_ebi_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedCenterNameSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, databaseSample, CenterNameRule.INSTANCE));
    }

    @Test
    public void test_biosamples_rule_fixes_applicable_ebi_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedEbiBiosamplesSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, databaseSample, BioSamplesIdRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_fixes_applicable_ebi_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedEbiBrokerSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, databaseSample, BrokerRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_fixes_applicable_ncbi_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedNcbiBrokerSampleXml), enaXmlEnhancer.applyRules(ncbiSampleXml, databaseSample, BrokerRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_fixes_applicable_ddbj_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedDdbjBrokerSampleXml), enaXmlEnhancer.applyRules(ddbjSampleXml, databaseSample, BrokerRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_does_not_change_non_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaXmlEnhancer.getDatabaseSample(), BrokerRule.INSTANCE));
    }

    @Test
    public void test_alias_rule_fixes_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedMissingAliasSampleXml), enaXmlEnhancer.applyRules(missingAliasSampleXml, databaseSample, AliasRule.INSTANCE));
    }

    @Test
    public void test_alias_rule_does_not_change_non_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, databaseSample, AliasRule.INSTANCE));
    }

    @Test
    public void test_namespace_rule_fixes_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(missingNamespaceSampleXml, databaseSample, NamespaceRule.INSTANCE));
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(emptyNamespaceSampleXml, databaseSample, NamespaceRule.INSTANCE));
    }

    @Test
    public void test_namespace_rule_does_not_change_non_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, databaseSample, NamespaceRule.INSTANCE));
    }

    @Test
    @Ignore
    public void test_link_removal_rule_fixes_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedNcbiLinksRemoved), enaXmlEnhancer.applyRules(ncbiSampleXml, databaseSample, LinkRemovalRule.INSTANCE));
    }

    @Test
    public void test_first_public_and_last_updated_for_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXmlWithDates), enaXmlEnhancer.applyRules(exampleSampleXml, databaseSample, DatesRule.INSTANCE));
    }

}
