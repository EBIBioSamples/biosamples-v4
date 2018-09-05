package uk.ac.ebi.biosamples.ena;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, properties = {"job.autorun.enabled=false"})
public class EnaElementConverterTest {

    @Autowired
    private EnaElementConverter enaElementConverter;

    @Test
    public void test_alias_rule_fixes_applicable_xml() throws SQLException, DocumentException, IOException {
        Rule aliasRule = new AliasRule();
        assertEquals(pretty(expectedModifiedMissingAliasSampleXml), applyRules(missingAliasSampleXml, aliasRule));
    }

    @Test
    public void test_alias_rule_does_not_change_non_applicable_xml() throws SQLException, DocumentException, IOException {
        Rule aliasRule = new AliasRule();
        assertEquals(pretty(exampleSampleXml), applyRules(exampleSampleXml, aliasRule));
    }

    @Test
    public void test_namespace_rule_fixes_applicable_xml() throws SQLException, DocumentException, IOException {
        Rule namespaceRule = new NamespaceRule();
        assertEquals(pretty(exampleSampleXml), applyRules(missingNamespaceSampleXml, namespaceRule));
        assertEquals(pretty(exampleSampleXml), applyRules(emptyNamespaceSampleXml, namespaceRule));
    }

    @Test
    public void test_namespace_rule_does_not_change_non_applicable_xml() throws SQLException, DocumentException, IOException {
        Rule namespaceRule = new NamespaceRule();
        assertEquals(pretty(exampleSampleXml), applyRules(exampleSampleXml, namespaceRule));
    }


    private String pretty(String xmlString) throws IOException, DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(new StringReader(xmlString));
        return pretty(document);
    }

    private String pretty(Document document) throws IOException {
        OutputFormat format = OutputFormat.createPrettyPrint();
        StringWriter outputWriter = new StringWriter();
        XMLWriter writer = new XMLWriter(outputWriter, format);
        writer.write(document);
        outputWriter.close();
        writer.close();
        return outputWriter.toString();
    }

    private String applyRules(String inputXml, Rule... rules) throws SQLException, DocumentException, IOException {
        Element element = getSampleElement(inputXml);
        Element modifiedElement = applyRules(element.createCopy(), rules);
        Document document = DocumentHelper.createDocument();
        document.setRootElement(modifiedElement);
        return pretty(document);
    }

    private Element applyRules(Element sampleElement, Rule... rules) {
        for (Rule rule : rules) {
            sampleElement = rule.apply(sampleElement);
        }
        return sampleElement;
    }

    public interface Rule {
        Element apply(Element element);
    }

    public class AliasRule implements Rule {

        @Override
        public Element apply(Element root) {
            if (sampleAttributeIsMissing(root, "alias")) {
                XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(root).path("SAMPLE", "IDENTIFIERS", "SUBMITTER_ID");
                if (xmlPathBuilder.exists()) {
                    Node node = root.selectSingleNode("SAMPLE/IDENTIFIERS/SUBMITTER_ID");
                    node.detach();
                }
            }
            return root;
        }
    }

    public class NamespaceRule implements Rule {

        @Override
        public Element apply(Element root) {
            if (sampleAttributeIsMissing(root, "center_name")) {
                return root;
            }
            XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(root).path("SAMPLE", "IDENTIFIERS", "SUBMITTER_ID");
            if (xmlPathBuilder.exists()) {
                if (xmlPathBuilder.attributeExists("namespace") && !xmlPathBuilder.attribute("namespace").isEmpty()) {
                    return root;
                } else {
                    String centerName = XmlPathBuilder.of(root).path("SAMPLE").attribute("center_name");
                    xmlPathBuilder.element().addAttribute("namespace", centerName);
                    return root;
                }
            }
            return root;
        }
    }

    private boolean sampleAttributeIsMissing(Element element, String attribute) {
        return !XmlPathBuilder.of(element).path("SAMPLE").attributeExists(attribute);
    }

    private Element getSampleElement(String xmlString) throws SQLException, DocumentException {
        SAXReader reader = new SAXReader();
        Document xml = reader.read(new StringReader(xmlString));
        return xml.getRootElement();
    }

    private String exampleSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE alias=\"K510\" center_name=\"UNIBE-IG\" accession=\"ERS2295402\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>ERS2295402</PRIMARY_ID>\n" +
            "         <SUBMITTER_ID namespace=\"UNIBE-IG\">K510</SUBMITTER_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>unknown/stray cat</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9685</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Felis catus</SCIENTIFIC_NAME>\n" +
            "         <COMMON_NAME>domestic cat</COMMON_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>unknown/stray cat female</DESCRIPTION>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>ENA-CHECKLIST</TAG>\n" +
            "            <VALUE>ERC000011</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    private String missingAliasSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE center_name=\"UNIBE-IG\" accession=\"ERS2295402\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>ERS2295402</PRIMARY_ID>\n" +
            "         <SUBMITTER_ID namespace=\"UNIBE-IG\">K510</SUBMITTER_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>unknown/stray cat</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9685</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Felis catus</SCIENTIFIC_NAME>\n" +
            "         <COMMON_NAME>domestic cat</COMMON_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>unknown/stray cat female</DESCRIPTION>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>ENA-CHECKLIST</TAG>\n" +
            "            <VALUE>ERC000011</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    private String expectedModifiedMissingAliasSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE center_name=\"UNIBE-IG\" accession=\"ERS2295402\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>ERS2295402</PRIMARY_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>unknown/stray cat</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9685</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Felis catus</SCIENTIFIC_NAME>\n" +
            "         <COMMON_NAME>domestic cat</COMMON_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>unknown/stray cat female</DESCRIPTION>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>ENA-CHECKLIST</TAG>\n" +
            "            <VALUE>ERC000011</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    private String missingNamespaceSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE alias=\"K510\" center_name=\"UNIBE-IG\" accession=\"ERS2295402\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>ERS2295402</PRIMARY_ID>\n" +
            "         <SUBMITTER_ID>K510</SUBMITTER_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>unknown/stray cat</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9685</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Felis catus</SCIENTIFIC_NAME>\n" +
            "         <COMMON_NAME>domestic cat</COMMON_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>unknown/stray cat female</DESCRIPTION>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>ENA-CHECKLIST</TAG>\n" +
            "            <VALUE>ERC000011</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

    private String emptyNamespaceSampleXml = "<?xml version = '1.0' encoding = 'UTF-8'?><SAMPLE_SET>\n" +
            "   <SAMPLE alias=\"K510\" center_name=\"UNIBE-IG\" accession=\"ERS2295402\">\n" +
            "      <IDENTIFIERS>\n" +
            "         <PRIMARY_ID>ERS2295402</PRIMARY_ID>\n" +
            "         <SUBMITTER_ID namespace=\"\">K510</SUBMITTER_ID>\n" +
            "      </IDENTIFIERS>\n" +
            "      <TITLE>unknown/stray cat</TITLE>\n" +
            "      <SAMPLE_NAME>\n" +
            "         <TAXON_ID>9685</TAXON_ID>\n" +
            "         <SCIENTIFIC_NAME>Felis catus</SCIENTIFIC_NAME>\n" +
            "         <COMMON_NAME>domestic cat</COMMON_NAME>\n" +
            "      </SAMPLE_NAME>\n" +
            "      <DESCRIPTION>unknown/stray cat female</DESCRIPTION>\n" +
            "      <SAMPLE_ATTRIBUTES>\n" +
            "         <SAMPLE_ATTRIBUTE>\n" +
            "            <TAG>ENA-CHECKLIST</TAG>\n" +
            "            <VALUE>ERC000011</VALUE>\n" +
            "         </SAMPLE_ATTRIBUTE>\n" +
            "      </SAMPLE_ATTRIBUTES>\n" +
            "   </SAMPLE>\n" +
            "</SAMPLE_SET>";

}
