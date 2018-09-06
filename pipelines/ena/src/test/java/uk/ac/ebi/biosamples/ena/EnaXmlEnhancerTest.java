package uk.ac.ebi.biosamples.ena;

import org.dom4j.DocumentException;
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
import static uk.ac.ebi.biosamples.ena.EnaXmlEnhancer.*;
import static uk.ac.ebi.biosamples.ena.EnaXmlUtil.pretty;
import static uk.ac.ebi.biosamples.ena.ExampleSamples.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, properties = {"job.autorun.enabled=false"})
public class EnaXmlEnhancerTest {

    @Autowired
    private EnaElementConverter enaElementConverter;

    @Autowired
    private EnaXmlEnhancer enaXmlEnhancer;

    @Test
    public void test_broker_rule_fixes_applicable_ncbi_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedNcbiBrokerSampleXml), enaXmlEnhancer.applyRules(ncbiSampleXml, BrokerRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_fixes_applicable_ddbj_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedDdbjBrokerSampleXml), enaXmlEnhancer.applyRules(ddbjSampleXml, BrokerRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_does_not_change_non_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, BrokerRule.INSTANCE));
    }

    @Test
    public void test_alias_rule_fixes_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(expectedModifiedMissingAliasSampleXml), enaXmlEnhancer.applyRules(missingAliasSampleXml, AliasRule.INSTANCE));
    }

    @Test
    public void test_alias_rule_does_not_change_non_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, AliasRule.INSTANCE));
    }

    @Test
    public void test_namespace_rule_fixes_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(missingNamespaceSampleXml, NamespaceRule.INSTANCE));
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(emptyNamespaceSampleXml, NamespaceRule.INSTANCE));
    }

    @Test
    public void test_namespace_rule_does_not_change_non_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, NamespaceRule.INSTANCE));
    }

    @Test
    @Ignore
    public void test_link_removal_rule_fixes_applicable_xml() throws SQLException, DocumentException, IOException {
        assertEquals(pretty(ExampleSamples.expectedModifiedNcbiLinksRemoved), enaXmlEnhancer.applyRules(ncbiSampleXml, LinkRemovalRule.INSTANCE));
    }
}
