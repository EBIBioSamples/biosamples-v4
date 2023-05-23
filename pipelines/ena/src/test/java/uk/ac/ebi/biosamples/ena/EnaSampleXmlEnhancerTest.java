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
import static uk.ac.ebi.biosamples.ena.ExampleSamples.*;
import static uk.ac.ebi.biosamples.service.EnaXmlUtil.pretty;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.service.EnaSampleXmlEnhancer;
import uk.ac.ebi.biosamples.service.EraProDao;
import uk.ac.ebi.biosamples.service.EraproSample;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {TestApplication.class, EnaSampleXmlEnhancer.class, EraProDao.class},
    properties = {"job.autorun.enabled=false"})
public class EnaSampleXmlEnhancerTest {

  @Autowired private EnaSampleXmlEnhancer enaSampleXmlEnhancer;

  private EraproSample eraproSample;

  @Before
  public void setup() {
    eraproSample = new EraproSample();
    eraproSample.lastUpdated = "2015-06-23";
    eraproSample.firstPublic = "2010-02-26";
    eraproSample.brokerName = null;
    eraproSample.biosampleId = "SAMN00001603";
    eraproSample.centreName = "1000G";
    eraproSample.fixed = "N";
    eraproSample.taxId = Long.valueOf(9606);
    eraproSample.scientificName = "Homo sapiens";
    eraproSample.fixedTaxId = null;
    eraproSample.fixedCommonName = null;
    eraproSample.fixedScientificName = null;
  }

  @Test
  public void test_xml_with_all_rules() {
    assertEquals(
        expectedFullSampleXml, enaSampleXmlEnhancer.applyAllRules(fullSampleXml, eraproSample));
  }

  @Test
  public void test_center_name_rule_fixes_applicable_ebi_xml() {
    eraproSample.centreName = "expanded center name";
    assertEquals(
        pretty(expectedModifiedCenterNameSampleXml),
        enaSampleXmlEnhancer.applyRules(
            exampleSampleXml, eraproSample, EnaSampleXmlEnhancer.CenterNameRule.INSTANCE));
  }

  @Test
  public void test_biosamples_rule_fixes_applicable_ebi_xml() {
    assertEquals(
        expectedModifiedEbiBiosamplesSampleXml,
        enaSampleXmlEnhancer.applyRules(
            exampleSampleXml, eraproSample, EnaSampleXmlEnhancer.BioSamplesIdRule.INSTANCE));
  }

  @Test
  public void test_broker_rule_fixes_applicable_ebi_xml() {
    eraproSample.brokerName = "broker";
    assertEquals(
        pretty(expectedModifiedEbiBrokerSampleXml),
        enaSampleXmlEnhancer.applyRules(
            exampleSampleXml, eraproSample, EnaSampleXmlEnhancer.BrokerRule.INSTANCE));
  }

  @Test
  public void test_broker_rule_fixes_applicable_ncbi_xml() {
    assertEquals(
        pretty(expectedModifiedNcbiBrokerSampleXml),
        enaSampleXmlEnhancer.applyRules(
            ncbiSampleXml, eraproSample, EnaSampleXmlEnhancer.BrokerRule.INSTANCE));
  }

  @Test
  public void test_broker_rule_fixes_applicable_ddbj_xml() {
    assertEquals(
        pretty(expectedModifiedDdbjBrokerSampleXml),
        enaSampleXmlEnhancer.applyRules(
            ddbjSampleXml, eraproSample, EnaSampleXmlEnhancer.BrokerRule.INSTANCE));
  }

  @Test
  public void test_broker_rule_does_not_change_non_applicable_xml() {
    eraproSample = new EraproSample();
    eraproSample.lastUpdated = "2015-06-23";
    eraproSample.firstPublic = "2010-02-26";
    eraproSample.brokerName = null;
    eraproSample.biosampleId = "";
    eraproSample.centreName = "1000G";
    eraproSample.fixed = "N";
    eraproSample.taxId = Long.valueOf("9606");
    eraproSample.scientificName = "Homo sapiens";
    eraproSample.fixedTaxId = null;
    eraproSample.fixedCommonName = null;
    eraproSample.fixedScientificName = null;
    assertEquals(
        pretty(exampleSampleXml),
        enaSampleXmlEnhancer.applyRules(
            exampleSampleXml, eraproSample, EnaSampleXmlEnhancer.BrokerRule.INSTANCE));
  }

  @Test
  public void test_alias_rule_fixes_applicable_xml() {
    assertEquals(
        pretty(expectedModifiedMissingAliasSampleXml),
        enaSampleXmlEnhancer.applyRules(
            missingAliasSampleXml, eraproSample, EnaSampleXmlEnhancer.AliasRule.INSTANCE));
  }

  @Test
  public void test_alias_rule_does_not_change_non_applicable_xml() {
    assertEquals(
        pretty(exampleSampleXml),
        enaSampleXmlEnhancer.applyRules(
            exampleSampleXml, eraproSample, EnaSampleXmlEnhancer.AliasRule.INSTANCE));
  }

  @Test
  public void test_namespace_rule_fixes_applicable_xml() {
    assertEquals(
        pretty(exampleSampleXml),
        enaSampleXmlEnhancer.applyRules(
            missingNamespaceSampleXml, eraproSample, EnaSampleXmlEnhancer.NamespaceRule.INSTANCE));
    assertEquals(
        pretty(exampleSampleXml),
        enaSampleXmlEnhancer.applyRules(
            emptyNamespaceSampleXml, eraproSample, EnaSampleXmlEnhancer.NamespaceRule.INSTANCE));
  }

  @Test
  public void test_namespace_rule_does_not_change_non_applicable_xml() {
    assertEquals(
        pretty(exampleSampleXml),
        enaSampleXmlEnhancer.applyRules(
            exampleSampleXml, eraproSample, EnaSampleXmlEnhancer.NamespaceRule.INSTANCE));
  }

  @Test
  public void test_link_removal_rule_fixes_applicable_xml() {
    assertEquals(
        expectedModifiedNcbiLinksRemoved,
        enaSampleXmlEnhancer.applyRules(
            ncbiSampleXml, eraproSample, EnaSampleXmlEnhancer.LinkRemovalRule.INSTANCE));
  }

  @Test
  public void test_first_public_and_last_updated_for_applicable_xml() {
    eraproSample.lastUpdated = "2018-02-01";
    eraproSample.firstPublic = "2018-01-01";
    assertEquals(
        exampleSampleXmlWithDates,
        enaSampleXmlEnhancer.applyRules(
            exampleSampleXml, eraproSample, EnaSampleXmlEnhancer.DatesRule.INSTANCE));
  }

  @Test
  public void test_title_rule_fixes_applicable_xml() {
    eraproSample = new EraproSample();
    eraproSample.lastUpdated = "2018-03-09";
    eraproSample.firstPublic = "2010-02-26";
    eraproSample.brokerName = null;
    eraproSample.biosampleId = "'SAMEA749880'";
    eraproSample.centreName = "Wellcome Sanger Institute";
    eraproSample.fixed = "N";
    eraproSample.taxId = Long.valueOf("580240");
    eraproSample.scientificName = "Saccharomyces cerevisiae W303";
    eraproSample.fixedTaxId = null;
    eraproSample.fixedCommonName = null;
    eraproSample.fixedScientificName = null;
    assertEquals(
        exampleSampleWithTitleAddedXml,
        enaSampleXmlEnhancer.applyRules(
            exampleSampleWithoutTitleXml, eraproSample, EnaSampleXmlEnhancer.TitleRule.INSTANCE));
  }

  @Test
  public void test_taxon_fix_rule_fixes_applicable_xml() {
    eraproSample = new EraproSample();
    eraproSample.lastUpdated = "2015-06-23";
    eraproSample.firstPublic = "2010-02-26";
    eraproSample.brokerName = null;
    eraproSample.biosampleId = "'SAMN00014227'";
    eraproSample.centreName = "Baylor College of Medicine";
    eraproSample.fixed = "Y";
    eraproSample.taxId = Long.valueOf("7227");
    eraproSample.scientificName = null;
    eraproSample.fixedTaxId = "7227";
    eraproSample.fixedCommonName = null;
    eraproSample.fixedScientificName = "Drosophila melanogaster";
    assertEquals(
        exampleSampleThatHasBeenTaxonFixed,
        enaSampleXmlEnhancer.applyRules(
            exampleSampleThatCanBeTaxonFixed,
            eraproSample,
            EnaSampleXmlEnhancer.TaxonRule.INSTANCE));
  }

  @Test
  public void test_taxon_fix_rule_fixes_applicable_xml_SAMN02356578() {
    eraproSample.lastUpdated = "2015-06-23";
    eraproSample.firstPublic = "2013-09-25";
    eraproSample.brokerName = null;
    eraproSample.biosampleId = "'SAMN02356578'";
    eraproSample.centreName = "Broad Institute";
    eraproSample.fixed = "Y";
    eraproSample.taxId = Long.valueOf("1400346");
    eraproSample.scientificName = "Acinetobacter lwoffii NIPH 512";
    eraproSample.fixedTaxId = "981327";
    eraproSample.fixedCommonName = null;
    eraproSample.fixedScientificName = "Acinetobacter lwoffii NCTC 5866 = CIP 64.10 = NIPH 512";
    assertEquals(
        exampleSampleThatHasBeenTaxonFixedSAMN02356578,
        enaSampleXmlEnhancer.applyRules(
            exampleSampleThatCanBeTaxonFixedSAMN02356578,
            eraproSample,
            EnaSampleXmlEnhancer.TaxonRule.INSTANCE));
  }

  @Test
  public void test_pretty() {
    final String pretty1 = pretty(expectedModifiedNcbiLinksRemoved);
    final String pretty2 = pretty(pretty1);
    assertEquals(pretty1, pretty2);
  }
}
