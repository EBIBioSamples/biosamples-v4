package uk.ac.ebi.biosamples.ena;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.ena.EnaXmlEnhancer.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static uk.ac.ebi.biosamples.ena.EnaXmlUtil.pretty;
import static uk.ac.ebi.biosamples.ena.ExampleSamples.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, properties = {"job.autorun.enabled=false"})
public class EnaXmlEnhancerTest {

    @Autowired
    private EnaXmlEnhancer enaXmlEnhancer;

    private EnaDatabaseSample enaDatabaseSample;

    @Before
    public void setup() {
        enaDatabaseSample = enaXmlEnhancer.getEnaDatabaseSample("SAMN00001603");
    }

    @Test
    public void test_xml_with_all_rules() {
        assertEquals(expectedFullSampleXml, enaXmlEnhancer.applyAllRules(fullSampleXml, enaDatabaseSample));
    }

    @Test
    public void test_center_name_rule_fixes_applicable_ebi_xml() {
        enaDatabaseSample.centreName = "expanded center name";
        assertEquals(pretty(expectedModifiedCenterNameSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, CenterNameRule.INSTANCE));
    }

    @Test
    public void test_biosamples_rule_fixes_applicable_ebi_xml() {
        assertEquals(expectedModifiedEbiBiosamplesSampleXml, enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, BioSamplesIdRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_fixes_applicable_ebi_xml() {
        enaDatabaseSample.brokerName = "broker";
        assertEquals(pretty(expectedModifiedEbiBrokerSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, BrokerRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_fixes_applicable_ncbi_xml() {
        assertEquals(pretty(expectedModifiedNcbiBrokerSampleXml), enaXmlEnhancer.applyRules(ncbiSampleXml, enaDatabaseSample, BrokerRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_fixes_applicable_ddbj_xml() {
        assertEquals(pretty(expectedModifiedDdbjBrokerSampleXml), enaXmlEnhancer.applyRules(ddbjSampleXml, enaDatabaseSample, BrokerRule.INSTANCE));
    }

    @Test
    public void test_broker_rule_does_not_change_non_applicable_xml() {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaXmlEnhancer.getEnaDatabaseSample(""), BrokerRule.INSTANCE));
    }

    @Test
    public void test_alias_rule_fixes_applicable_xml() {
        assertEquals(pretty(expectedModifiedMissingAliasSampleXml), enaXmlEnhancer.applyRules(missingAliasSampleXml, enaDatabaseSample, AliasRule.INSTANCE));
    }

    @Test
    public void test_alias_rule_does_not_change_non_applicable_xml() {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, AliasRule.INSTANCE));
    }

    @Test
    public void test_namespace_rule_fixes_applicable_xml() {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(missingNamespaceSampleXml, enaDatabaseSample, NamespaceRule.INSTANCE));
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(emptyNamespaceSampleXml, enaDatabaseSample, NamespaceRule.INSTANCE));
    }

    @Test
    public void test_namespace_rule_does_not_change_non_applicable_xml() {
        assertEquals(pretty(exampleSampleXml), enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, NamespaceRule.INSTANCE));
    }

    @Test
    public void test_link_removal_rule_fixes_applicable_xml() {
        assertEquals(expectedModifiedNcbiLinksRemoved, enaXmlEnhancer.applyRules(ncbiSampleXml, enaDatabaseSample, LinkRemovalRule.INSTANCE));
    }

    @Test
    public void test_first_public_and_last_updated_for_applicable_xml() {
        enaDatabaseSample.lastUpdated = "2018-02-01";
        enaDatabaseSample.firstPublic = "2018-01-01";
        assertEquals(exampleSampleXmlWithDates, enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, DatesRule.INSTANCE));
    }

    @Test
    public void test_title_rule_fixes_applicable_xml() {
        enaDatabaseSample = enaXmlEnhancer.getEnaDatabaseSample("SAMEA749880");
        assertEquals(exampleSampleWithTitleAddedXml, enaXmlEnhancer.applyRules(exampleSampleWithoutTitleXml, enaDatabaseSample, TitleRule.INSTANCE));
    }

    @Test
    public void test_taxon_fix_rule_fixes_applicable_xml() {
        enaDatabaseSample = enaXmlEnhancer.getEnaDatabaseSample("SAMN00014227");
        assertEquals(exampleSampleThatHasBeenTaxonFixed, enaXmlEnhancer.applyRules(exampleSampleThatCanBeTaxonFixed, enaDatabaseSample, TaxonRule.INSTANCE));
    }

    @Test
    public void test_taxon_fix_rule_fixes_applicable_xml_SAMN02356578() {
        enaDatabaseSample = enaXmlEnhancer.getEnaDatabaseSample("SAMN02356578");
        assertEquals(exampleSampleThatHasBeenTaxonFixedSAMN02356578, enaXmlEnhancer.applyRules(exampleSampleThatCanBeTaxonFixedSAMN02356578, enaDatabaseSample, TaxonRule.INSTANCE));
    }

    @Test
    public void test_pretty() {
        String pretty1 = pretty(expectedModifiedNcbiLinksRemoved);
        String pretty2 = pretty(pretty1);
        assertEquals(pretty1, pretty2);

    }
}
