package uk.ac.ebi.biosamples.ena;

import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;

import static uk.ac.ebi.biosamples.ena.EnaXmlUtil.*;

@Service
public class EnaXmlEnhancer {

    public String applyRules(String inputXml, Rule... rules) throws SQLException, DocumentException, IOException {
        Element element = getSampleElement(inputXml);
        Element modifiedElement = applyRules(element.createCopy(), rules);
        Document document = DocumentHelper.createDocument();
        document.setRootElement(modifiedElement);
        return pretty(document);
    }

    public Element applyRules(Element sampleElement, Rule... rules) {
        for (Rule rule : rules) {
            sampleElement = rule.apply(sampleElement);
        }
        return sampleElement;
    }

    public interface Rule {
        Element apply(Element element);
    }

    public enum AliasRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element root) {
            if (!XmlPathBuilder.of(root).path("SAMPLE").attributeExists("alias")) {
                XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(root).path("SAMPLE", "IDENTIFIERS", "SUBMITTER_ID");
                if (xmlPathBuilder.exists()) {
                    Node node = root.selectSingleNode("SAMPLE/IDENTIFIERS/SUBMITTER_ID");
                    node.detach();
                }
            }
            return root;
        }
    }

    public enum NamespaceRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element root) {
            if (!XmlPathBuilder.of(root).path("SAMPLE").attributeExists("center_name")) {
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

    public enum BrokerRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element root) {
            XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(root).path("SAMPLE");
            if (xmlPathBuilder.attributeExists("accession")) {
                String accession = xmlPathBuilder.attribute("accession");
                if (accession.startsWith("ERS")) {
                    String brokerName = getBrokerNameFromEraPro(accession);
                    if (brokerName != null) {
                        xmlPathBuilder.element().addAttribute("broker_name", "NCBI");
                    }
                    return root;
                }
                if (accession.startsWith("SRS")) {
                    xmlPathBuilder.element().addAttribute("broker_name", "NCBI");
                    return root;
                }
                if (accession.startsWith("DRS")) {
                    xmlPathBuilder.element().addAttribute("broker_name", "DDBJ");
                    return root;
                }
            }
            return root;
        }

        private String getBrokerNameFromEraPro(String accession) {
            return null;
        }
    }

    public enum LinkRemovalRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element root) {
            XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(root).path("SAMPLE", "SAMPLE_LINKS");
            if (xmlPathBuilder.exists()) {
                for (Element sampleLinkElement : xmlPathBuilder.elements("SAMPLE_LINK")) {
                    if (sampleLinkElement.element("URL_LINK") != null) {
                        sampleLinkElement.detach();
                    }
                }
            }
            return root;
        }
    }

    private Element getSampleElement(String xmlString) throws SQLException, DocumentException {
        SAXReader reader = new SAXReader();
        Document xml = reader.read(new StringReader(xmlString));
        return xml.getRootElement();
    }
}
