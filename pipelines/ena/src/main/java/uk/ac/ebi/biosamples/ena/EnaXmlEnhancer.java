package uk.ac.ebi.biosamples.ena;

import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;

import static uk.ac.ebi.biosamples.ena.EnaXmlUtil.pretty;

@Service
public class EnaXmlEnhancer {

    private EraProDao eraProDao;

    private static final Logger LOGGER = LoggerFactory.getLogger(EnaXmlEnhancer.class);

    public EnaXmlEnhancer(EraProDao eraProDao) {
        this.eraProDao = eraProDao;
    }

    public String applyRules(String inputXml, EnaDatabaseSample enaDatabaseSample, Rule... rules) {
        Element element = getSampleElement(inputXml);
        Element modifiedElement = applyRules(element.createCopy(), enaDatabaseSample, rules);
        Document document = DocumentHelper.createDocument();
        document.setRootElement(modifiedElement);
        return pretty(document);
    }

    public EnaDatabaseSample getEnaDatabaseSample(String accession) {
        EnaDatabaseSample enaDatabaseSample = new EnaDatabaseSample();
        RowCallbackHandler rch = new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet resultSet) {
                try {
                    enaDatabaseSample.bioSamplesId = resultSet.getString("BIOSAMPLE_ID");
                    enaDatabaseSample.brokerName = resultSet.getString("BROKER_NAME");
                    enaDatabaseSample.centreName = resultSet.getString("CENTER_NAME");
                    enaDatabaseSample.lastUpdated = resultSet.getString("LAST_UPDATED");
                    enaDatabaseSample.firstPublic = resultSet.getString("FIRST_PUBLIC");
                } catch (SQLException e) {
                    LOGGER.error("Error processing database result", e);
                }

            }
        };
        eraProDao.getEnaDatabaseSample(accession, rch);
        return enaDatabaseSample;
    }

    private Element applyRules(Element sampleElement, EnaDatabaseSample enaDatabaseSample, Rule... rules) {
        for (Rule rule : rules) {
            sampleElement = rule.apply(sampleElement, enaDatabaseSample);
        }
        return sampleElement;
    }

    public String applyAllRules(String inputXml, EnaDatabaseSample enaDatabaseSample){
        return applyRules(inputXml, enaDatabaseSample, AliasRule.INSTANCE, NamespaceRule.INSTANCE, BrokerRule.INSTANCE, LinkRemovalRule.INSTANCE, CenterNameRule.INSTANCE, DatesRule.INSTANCE, BioSamplesIdRule.INSTANCE);
    }

    public Element applyAllRules(Element element, EnaDatabaseSample enaDatabaseSample) {
        return applyRules(element, enaDatabaseSample, AliasRule.INSTANCE, NamespaceRule.INSTANCE, BrokerRule.INSTANCE, LinkRemovalRule.INSTANCE, CenterNameRule.INSTANCE, DatesRule.INSTANCE, BioSamplesIdRule.INSTANCE);
    }

    public interface Rule {
        Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample);
    }

    public enum AliasRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample) {
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
        public Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample) {
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
        public Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample) {
            XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE");
            if (xmlPathBuilder.attributeExists("accession")) {
                String accession = xmlPathBuilder.attribute("accession");
                if (accession.startsWith("ERS")) {
                    if (enaDatabaseSample.brokerName != null) {
                        xmlPathBuilder.element().addAttribute("broker_name", enaDatabaseSample.brokerName);
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
        public Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample) {
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

    public enum CenterNameRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample) {
            if (enaDatabaseSample.centreName != null) {
                XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE");
                if (xmlPathBuilder.attributeExists("center_name")) {
                    xmlPathBuilder.element().setAttributeValue("center_name", enaDatabaseSample.centreName);
                }
            }
            return sampleXml;
        }
    }

    public enum DatesRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample) {
            if (enaDatabaseSample.firstPublic == null || enaDatabaseSample.lastUpdated == null) {
                return sampleXml;
            }
            XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_ATTRIBUTES");
            if (xmlPathBuilder.exists()) {
                xmlPathBuilder.element().add(createSampleAttribute("ENA-FIRST-PUBLIC", enaDatabaseSample.firstPublic));
                xmlPathBuilder.element().add(createSampleAttribute("ENA-LAST-UPDATE", enaDatabaseSample.lastUpdated));
            }
            return sampleXml;
        }
    }

    public enum BioSamplesIdRule implements Rule {

        INSTANCE;

        @Override
        public Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample) {
            if (enaDatabaseSample.bioSamplesId == null) {
                return sampleXml;
            }
            XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE", "IDENTIFIERS");
            if (xmlPathBuilder.exists()) {
                boolean bioSamplesExternalIdExists = false;
                for (Element element : xmlPathBuilder.elements("EXTERNAL_ID")) {
                    if (element.attribute("namespace").getText().equals("BioSample")) {
                        bioSamplesExternalIdExists = true;
                    }
                }
                if (!bioSamplesExternalIdExists) {
                    xmlPathBuilder.element().add(createExternalRef("BioSample", enaDatabaseSample.bioSamplesId));
                }
            }
            return sampleXml;
        }
    }

    private Element getSampleElement(String xmlString) {
        SAXReader reader = new SAXReader();
        Document xml = null;
        try {
            xml = reader.read(new StringReader(xmlString));
        } catch (DocumentException e) {
            LOGGER.error("Error reading XML", e);
        }
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
        tagElement.setText(tag);
        sampleAttributeElement.add(tagElement);
        Element valueElement = DocumentHelper.createElement("VALUE");
        valueElement.setText(value);
        sampleAttributeElement.add(valueElement);
        return sampleAttributeElement;
    }
}
