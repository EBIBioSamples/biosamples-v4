/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.ena;

import static org.junit.Assert.assertEquals;
import static uk.ac.ebi.biosamples.ena.EnaXmlUtil.pretty;
import static uk.ac.ebi.biosamples.ena.ExampleSamples.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.ena.EnaXmlEnhancer.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {TestApplication.class, EnaXmlEnhancer.class, EraProDao.class},
    properties = {"job.autorun.enabled=false"})
public class EnaXmlEnhancerTest {

  @Autowired private EnaXmlEnhancer enaXmlEnhancer;

  private EnaDatabaseSample enaDatabaseSample;

  @Before
  public void setup() {
    enaDatabaseSample = new EnaDatabaseSample();
    enaDatabaseSample.lastUpdated = "2015-06-23";
    enaDatabaseSample.firstPublic = "2010-02-26";
    enaDatabaseSample.brokerName = null;
    enaDatabaseSample.bioSamplesId = "SAMN00001603";
    enaDatabaseSample.centreName = "1000G";
    enaDatabaseSample.fixed = "N";
    enaDatabaseSample.taxId = "9606";
    enaDatabaseSample.scientificName = "Homo sapiens";
    enaDatabaseSample.fixedTaxId = null;
    enaDatabaseSample.fixedCommonName = null;
    enaDatabaseSample.fixedScientificName = null;
  }

  @Test
  public void test_xml_with_all_rules() {
    assertEquals(
        expectedFullSampleXml, enaXmlEnhancer.applyAllRules(fullSampleXml, enaDatabaseSample));
  }

  @Test
  public void test_center_name_rule_fixes_applicable_ebi_xml() {
    enaDatabaseSample.centreName = "expanded center name";
    assertEquals(
        pretty(expectedModifiedCenterNameSampleXml),
        enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, CenterNameRule.INSTANCE));
  }

  @Test
  public void test_biosamples_rule_fixes_applicable_ebi_xml() {
    assertEquals(
        expectedModifiedEbiBiosamplesSampleXml,
        enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, BioSamplesIdRule.INSTANCE));
  }

  @Test
  public void test_broker_rule_fixes_applicable_ebi_xml() {
    enaDatabaseSample.brokerName = "broker";
    assertEquals(
        pretty(expectedModifiedEbiBrokerSampleXml),
        enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, BrokerRule.INSTANCE));
  }

  @Test
  public void test_broker_rule_fixes_applicable_ncbi_xml() {
    assertEquals(
        pretty(expectedModifiedNcbiBrokerSampleXml),
        enaXmlEnhancer.applyRules(ncbiSampleXml, enaDatabaseSample, BrokerRule.INSTANCE));
  }

  @Test
  public void test_broker_rule_fixes_applicable_ddbj_xml() {
    assertEquals(
        pretty(expectedModifiedDdbjBrokerSampleXml),
        enaXmlEnhancer.applyRules(ddbjSampleXml, enaDatabaseSample, BrokerRule.INSTANCE));
  }

  @Test
  public void test_broker_rule_does_not_change_non_applicable_xml() {
    enaDatabaseSample = new EnaDatabaseSample();
    enaDatabaseSample.lastUpdated = "2015-06-23";
    enaDatabaseSample.firstPublic = "2010-02-26";
    enaDatabaseSample.brokerName = null;
    enaDatabaseSample.bioSamplesId = "";
    enaDatabaseSample.centreName = "1000G";
    enaDatabaseSample.fixed = "N";
    enaDatabaseSample.taxId = "9606";
    enaDatabaseSample.scientificName = "Homo sapiens";
    enaDatabaseSample.fixedTaxId = null;
    enaDatabaseSample.fixedCommonName = null;
    enaDatabaseSample.fixedScientificName = null;
    assertEquals(
        pretty(exampleSampleXml),
        enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, BrokerRule.INSTANCE));
  }

  @Test
  public void test_alias_rule_fixes_applicable_xml() {
    assertEquals(
        pretty(expectedModifiedMissingAliasSampleXml),
        enaXmlEnhancer.applyRules(missingAliasSampleXml, enaDatabaseSample, AliasRule.INSTANCE));
  }

  @Test
  public void test_alias_rule_does_not_change_non_applicable_xml() {
    assertEquals(
        pretty(exampleSampleXml),
        enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, AliasRule.INSTANCE));
  }

  @Test
  public void test_namespace_rule_fixes_applicable_xml() {
    assertEquals(
        pretty(exampleSampleXml),
        enaXmlEnhancer.applyRules(
            missingNamespaceSampleXml, enaDatabaseSample, NamespaceRule.INSTANCE));
    assertEquals(
        pretty(exampleSampleXml),
        enaXmlEnhancer.applyRules(
            emptyNamespaceSampleXml, enaDatabaseSample, NamespaceRule.INSTANCE));
  }

  @Test
  public void test_namespace_rule_does_not_change_non_applicable_xml() {
    assertEquals(
        pretty(exampleSampleXml),
        enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, NamespaceRule.INSTANCE));
  }

  @Test
  public void test_link_removal_rule_fixes_applicable_xml() {
    assertEquals(
        expectedModifiedNcbiLinksRemoved,
        enaXmlEnhancer.applyRules(ncbiSampleXml, enaDatabaseSample, LinkRemovalRule.INSTANCE));
  }

  @Test
  public void test_first_public_and_last_updated_for_applicable_xml() {
    enaDatabaseSample.lastUpdated = "2018-02-01";
    enaDatabaseSample.firstPublic = "2018-01-01";
    assertEquals(
        exampleSampleXmlWithDates,
        enaXmlEnhancer.applyRules(exampleSampleXml, enaDatabaseSample, DatesRule.INSTANCE));
  }

  @Test
  public void test_title_rule_fixes_applicable_xml() {
    enaDatabaseSample = new EnaDatabaseSample();
    enaDatabaseSample.lastUpdated = "2018-03-09";
    enaDatabaseSample.firstPublic = "2010-02-26";
    enaDatabaseSample.brokerName = null;
    enaDatabaseSample.bioSamplesId = "'SAMEA749880'";
    enaDatabaseSample.centreName = "Wellcome Sanger Institute";
    enaDatabaseSample.fixed = "N";
    enaDatabaseSample.taxId = "'580240'";
    enaDatabaseSample.scientificName = "Saccharomyces cerevisiae W303";
    enaDatabaseSample.fixedTaxId = null;
    enaDatabaseSample.fixedCommonName = null;
    enaDatabaseSample.fixedScientificName = null;
    assertEquals(
        exampleSampleWithTitleAddedXml,
        enaXmlEnhancer.applyRules(
            exampleSampleWithoutTitleXml, enaDatabaseSample, TitleRule.INSTANCE));
  }

  @Test
  public void test_taxon_fix_rule_fixes_applicable_xml() {
    enaDatabaseSample = new EnaDatabaseSample();
    enaDatabaseSample.lastUpdated = "2015-06-23";
    enaDatabaseSample.firstPublic = "2010-02-26";
    enaDatabaseSample.brokerName = null;
    enaDatabaseSample.bioSamplesId = "'SAMN00014227'";
    enaDatabaseSample.centreName = "Baylor College of Medicine";
    enaDatabaseSample.fixed = "Y";
    enaDatabaseSample.taxId = "'7227'";
    enaDatabaseSample.scientificName = null;
    enaDatabaseSample.fixedTaxId = "7227";
    enaDatabaseSample.fixedCommonName = null;
    enaDatabaseSample.fixedScientificName = "Drosophila melanogaster";
    assertEquals(
        exampleSampleThatHasBeenTaxonFixed,
        enaXmlEnhancer.applyRules(
            exampleSampleThatCanBeTaxonFixed, enaDatabaseSample, TaxonRule.INSTANCE));
  }

  @Test
  public void test_taxon_fix_rule_fixes_applicable_xml_SAMN02356578() {
    enaDatabaseSample.lastUpdated = "2015-06-23";
    enaDatabaseSample.firstPublic = "2013-09-25";
    enaDatabaseSample.brokerName = null;
    enaDatabaseSample.bioSamplesId = "'SAMN02356578'";
    enaDatabaseSample.centreName = "Broad Institute";
    enaDatabaseSample.fixed = "Y";
    enaDatabaseSample.taxId = "'1400346'";
    enaDatabaseSample.scientificName = "Acinetobacter lwoffii NIPH 512";
    enaDatabaseSample.fixedTaxId = "981327";
    enaDatabaseSample.fixedCommonName = null;
    enaDatabaseSample.fixedScientificName =
        "Acinetobacter lwoffii NCTC 5866 = CIP 64.10 = NIPH 512";
    assertEquals(
        exampleSampleThatHasBeenTaxonFixedSAMN02356578,
        enaXmlEnhancer.applyRules(
            exampleSampleThatCanBeTaxonFixedSAMN02356578, enaDatabaseSample, TaxonRule.INSTANCE));
  }

  @Test
  public void test_pretty() {
    String pretty1 = pretty(expectedModifiedNcbiLinksRemoved);
    String pretty2 = pretty(pretty1);
    assertEquals(pretty1, pretty2);
  }
}
