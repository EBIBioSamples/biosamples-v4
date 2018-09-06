package uk.ac.ebi.biosamples.ena;

import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static uk.ac.ebi.biosamples.ena.EnaXmlUtil.pretty;

@Service
public class EnaXmlEnhancer {

    public String applyRules(String inputXml, DatabaseSample databaseSample, Rule... rules) throws SQLException, DocumentException, IOException {
        Element element = getSampleElement(inputXml);
        Element modifiedElement = applyRules(element.createCopy(), databaseSample, rules);
        Document document = DocumentHelper.createDocument();
        document.setRootElement(modifiedElement);
        return pretty(document);
    }

    public DatabaseSample getDatabaseSample() {
        return new DatabaseSample();
    }

    private Element applyRules(Element sampleElement, DatabaseSample databaseSample, Rule... rules) {
        for (Rule rule : rules) {
            sampleElement = rule.apply(sampleElement, databaseSample);
        }
        return sampleElement;
    }

    public String applyAllRules(String inputXml, DatabaseSample databaseSample) throws DocumentException, SQLException, IOException {
        return applyRules(inputXml, databaseSample,AliasRule.INSTANCE, NamespaceRule.INSTANCE, BrokerRule.INSTANCE, LinkRemovalRule.INSTANCE, CenterNameRule.INSTANCE, DatesRule.INSTANCE, BioSamplesIdRule.INSTANCE );
    }

    public interface Rule {
        Element apply(Element sampleXml, DatabaseSample databaseSample);
    }

    public enum AliasRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element sampleXml, DatabaseSample databaseSample) {
            if (!XmlPathBuilder.of(sampleXml).path("SAMPLE").attributeExists("alias")) {
                XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE", "IDENTIFIERS", "SUBMITTER_ID");
                if (xmlPathBuilder.exists()) {
                    Node node = sampleXml.selectSingleNode("SAMPLE/IDENTIFIERS/SUBMITTER_ID");
                    node.detach();
                }
            }
            return sampleXml;
        }

    }

    public enum NamespaceRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element sampleXml, DatabaseSample databaseSample) {
            if (!XmlPathBuilder.of(sampleXml).path("SAMPLE").attributeExists("center_name")) {
                return sampleXml;
            }
            XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE", "IDENTIFIERS", "SUBMITTER_ID");
            if (xmlPathBuilder.exists()) {
                if (xmlPathBuilder.attributeExists("namespace") && !xmlPathBuilder.attribute("namespace").isEmpty()) {
                    return sampleXml;
                } else {
                    String centerName = XmlPathBuilder.of(sampleXml).path("SAMPLE").attribute("center_name");
                    xmlPathBuilder.element().addAttribute("namespace", centerName);
                    return sampleXml;
                }
            }
            return sampleXml;
        }
    }

    public enum BrokerRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element sampleXml, DatabaseSample databaseSample) {
            XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE");
            if (xmlPathBuilder.attributeExists("accession")) {
                String accession = xmlPathBuilder.attribute("accession");
                if (accession.startsWith("ERS")) {
                    if (databaseSample.brokerName != null) {
                        xmlPathBuilder.element().addAttribute("broker_name", databaseSample.brokerName);
                    }
                    return sampleXml;
                }
                if (accession.startsWith("SRS")) {
                    xmlPathBuilder.element().addAttribute("broker_name", "NCBI");
                    return sampleXml;
                }
                if (accession.startsWith("DRS")) {
                    xmlPathBuilder.element().addAttribute("broker_name", "DDBJ");
                    return sampleXml;
                }
            }
            return sampleXml;
        }
    }

    public enum LinkRemovalRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element sampleXml, DatabaseSample databaseSample) {
            XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_LINKS");
            if (xmlPathBuilder.exists()) {
                for (Element sampleLinkElement : xmlPathBuilder.elements("SAMPLE_LINK")) {
                    if (sampleLinkElement.element("URL_LINK") != null) {
                        sampleLinkElement.detach();
                    }
                }
            }
            return sampleXml;
        }
    }

    public class DatabaseSample {
        public String lastUpdated;
        public String firstPublic;
        public String brokerName;
        public String bioSamplesId;
        public String centreName;
    }

    public enum CenterNameRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element sampleXml, DatabaseSample databaseSample) {
            if (databaseSample.centreName != null) {
                XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE");
                if (xmlPathBuilder.attributeExists("center_name")) {
                    xmlPathBuilder.element().setAttributeValue("center_name", databaseSample.centreName);
                }
            }
            return sampleXml;
        }
    }

    public enum DatesRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element sampleXml, DatabaseSample databaseSample) {
            XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_ATTRIBUTES");
            if (xmlPathBuilder.exists()) {
                xmlPathBuilder.element().add(createSampleAttribute("ENA-FIRST-PUBLIC", databaseSample.firstPublic));
                xmlPathBuilder.element().add(createSampleAttribute("ENA-LAST-UPDATED", databaseSample.lastUpdated));
            }
            return sampleXml;
        }
    }

    public enum BioSamplesIdRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element sampleXml, DatabaseSample databaseSample) {
            if (databaseSample.bioSamplesId != null) {
                XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE", "IDENTIFIERS");
                if (xmlPathBuilder.exists()) {
                    xmlPathBuilder.element().add(createExternalRef("BioSample", databaseSample.bioSamplesId));
                }
                return sampleXml;
            }
            return sampleXml;
        }
    }

    private Element getSampleElement(String xmlString) throws SQLException, DocumentException {
        SAXReader reader = new SAXReader();
        Document xml = reader.read(new StringReader(xmlString));
        return xml.getRootElement();
    }

    private static Element createExternalRef(String namespace, String bioSamplesId) {
        Element externalIdElement = DocumentHelper.createElement("EXTERNAL_ID");
        externalIdElement.addAttribute("namespace", namespace);
        externalIdElement.setText(bioSamplesId);
        return externalIdElement;
    }

    private static Element createSampleAttribute(String tag, String value) {
        Element sampleAttributeElement = DocumentHelper.createElement("SAMPLE_ATTRIBUTE");
        Element tagElement = DocumentHelper.createElement("TAG");
        tagElement.setText("ENA-FIRST-PUBLIC");
        sampleAttributeElement.add(tagElement);
        Element valueElement = DocumentHelper.createElement("VALUE");
        valueElement.setText(value);
        sampleAttributeElement.add(valueElement);
        return sampleAttributeElement;
    }
}
