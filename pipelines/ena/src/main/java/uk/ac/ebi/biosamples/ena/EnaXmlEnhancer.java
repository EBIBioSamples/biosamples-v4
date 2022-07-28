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

import static uk.ac.ebi.biosamples.ena.EnaXmlUtil.pretty;

import java.io.StringReader;
import java.sql.SQLException;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

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
    RowCallbackHandler rch =
        resultSet -> {
          try {
            enaDatabaseSample.bioSamplesId = resultSet.getString("BIOSAMPLE_ID");
            enaDatabaseSample.brokerName = resultSet.getString("BROKER_NAME");
            enaDatabaseSample.centreName = resultSet.getString("CENTER_NAME");
            enaDatabaseSample.lastUpdated = resultSet.getString("LAST_UPDATED");
            enaDatabaseSample.firstPublic = resultSet.getString("FIRST_PUBLIC");
            enaDatabaseSample.fixed = resultSet.getString("FIXED");
            enaDatabaseSample.taxId = resultSet.getString("TAX_ID");
            enaDatabaseSample.scientificName = resultSet.getString("SCIENTIFIC_NAME");
            enaDatabaseSample.fixedTaxId = resultSet.getString("FIXED_TAX_ID");
            enaDatabaseSample.fixedCommonName = resultSet.getString("FIXED_COMMON_NAME");
            enaDatabaseSample.fixedScientificName = resultSet.getString("FIXED_SCIENTIFIC_NAME");
          } catch (SQLException e) {
            LOGGER.error("Error processing database result", e);
          }
        };
    eraProDao.getEnaDatabaseSample(accession, rch);
    return enaDatabaseSample;
  }

  private Element applyRules(
      Element sampleElement, EnaDatabaseSample enaDatabaseSample, Rule... rules) {
    for (Rule rule : rules) {
      sampleElement = rule.apply(sampleElement, enaDatabaseSample);
    }
    return sampleElement;
  }

  public String applyAllRules(String inputXml, EnaDatabaseSample enaDatabaseSample) {
    return applyRules(
        inputXml,
        enaDatabaseSample,
        AliasRule.INSTANCE,
        NamespaceRule.INSTANCE,
        BrokerRule.INSTANCE,
        LinkRemovalRule.INSTANCE,
        CenterNameRule.INSTANCE,
        DatesRule.INSTANCE,
        BioSamplesIdRule.INSTANCE);
  }

  public Element applyAllRules(Element element, EnaDatabaseSample enaDatabaseSample) {
    return applyRules(
        element,
        enaDatabaseSample,
        AliasRule.INSTANCE,
        NamespaceRule.INSTANCE,
        BrokerRule.INSTANCE,
        LinkRemovalRule.INSTANCE,
        CenterNameRule.INSTANCE,
        DatesRule.INSTANCE,
        BioSamplesIdRule.INSTANCE);
  }

  public interface Rule {
    Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample);
  }

  public enum AliasRule implements Rule {
    INSTANCE;

    @Override
    public Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample) {
      if (!XmlPathBuilder.of(sampleXml).path("SAMPLE").attributeExists("alias")) {
        XmlPathBuilder xmlPathBuilder =
            XmlPathBuilder.of(sampleXml).path("SAMPLE", "IDENTIFIERS", "SUBMITTER_ID");
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
      XmlPathBuilder xmlPathBuilder =
          XmlPathBuilder.of(sampleXml).path("SAMPLE", "IDENTIFIERS", "SUBMITTER_ID");
      if (xmlPathBuilder.exists()) {
        if (!xmlPathBuilder.attributeExists("namespace")
            || xmlPathBuilder.attribute("namespace").isEmpty()) {
          String centerName = XmlPathBuilder.of(sampleXml).path("SAMPLE").attribute("center_name");
          xmlPathBuilder.element().addAttribute("namespace", centerName);
        }
        return sampleXml;
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

    @SuppressWarnings("deprecation")
    @Override
    public Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample) {
      if (enaDatabaseSample.centreName != null) {
        XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE");
        if (xmlPathBuilder.attributeExists("center_name")) {
          xmlPathBuilder
              .element()
              .setAttributeValue("center_alias", xmlPathBuilder.attribute("center_name"));
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
      XmlPathBuilder xmlPathBuilder =
          XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_ATTRIBUTES");
      if (xmlPathBuilder.exists()) {
        xmlPathBuilder
            .element()
            .add(createSampleAttribute("ENA-FIRST-PUBLIC", enaDatabaseSample.firstPublic));
        xmlPathBuilder
            .element()
            .add(createSampleAttribute("ENA-LAST-UPDATE", enaDatabaseSample.lastUpdated));
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
          xmlPathBuilder.element().add(createExternalRef(enaDatabaseSample.bioSamplesId));
        }
      }
      return sampleXml;
    }
  }

  public enum TitleRule implements Rule {
    INSTANCE;

    @Override
    public Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample) {
      if (enaDatabaseSample.bioSamplesId == null) {
        return sampleXml;
      }
      XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE", "TITLE");
      if (!xmlPathBuilder.exists()) {
        String newTitle = null;
        if (enaDatabaseSample.fixed.equals("Y")) {
          if (enaDatabaseSample.fixedScientificName != null) {
            newTitle = enaDatabaseSample.fixedScientificName;
          }
        } else if (enaDatabaseSample.scientificName != null) {
          newTitle = enaDatabaseSample.scientificName;
        }
        if (newTitle != null) {
          xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE");
          Element titleElement = DocumentHelper.createElement("TITLE");
          titleElement.setText(newTitle);
          xmlPathBuilder.element().add(titleElement);
        }
      }
      return sampleXml;
    }
  }

  public enum TaxonRule implements Rule {
    INSTANCE;

    @Override
    public Element apply(Element sampleXml, EnaDatabaseSample enaDatabaseSample) {
      if (enaDatabaseSample.fixed.equals("Y")) {
        XmlPathBuilder parentXmlPathBuilderName =
            XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_NAME");
        if (!parentXmlPathBuilderName.exists()) {
          XmlPathBuilder.of(sampleXml).path("SAMPLE").element().addElement("SAMPLE_NAME");
        }
        XmlPathBuilder taxIdXmlPathBuilderTaxonId =
            XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_NAME", "TAXON_ID");
        String taxId = enaDatabaseSample.fixedTaxId == null ? "" : enaDatabaseSample.fixedTaxId;
        if (taxIdXmlPathBuilderTaxonId.exists()) {
          taxIdXmlPathBuilderTaxonId.element().setText(taxId);
        } else {
          parentXmlPathBuilderName.element().addElement("TAXON_ID", taxId);
        }
        XmlPathBuilder taxIdXmlPathBuilderScientificName =
            XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_NAME", "SCIENTIFIC_NAME");
        String scientificName =
            enaDatabaseSample.fixedScientificName == null
                ? ""
                : enaDatabaseSample.fixedScientificName;
        if (taxIdXmlPathBuilderScientificName.exists()) {
          taxIdXmlPathBuilderScientificName.element().setText(scientificName);
        } else {
          parentXmlPathBuilderName.element().addElement("SCIENTIFIC_NAME", scientificName);
        }
        XmlPathBuilder taxIdXmlPathBuilderCommonName =
            XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_NAME", "COMMON_NAME");
        String commonName =
            enaDatabaseSample.fixedCommonName == null ? "" : enaDatabaseSample.fixedCommonName;
        if (taxIdXmlPathBuilderCommonName.exists()) {
          taxIdXmlPathBuilderCommonName.element().setText(commonName);
        } else {
          parentXmlPathBuilderName.element().addElement("COMMON_NAME", commonName);
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
    assert xml != null;

    return xml.getRootElement();
  }

  private static Element createExternalRef(String bioSamplesId) {
    Element externalIdElement = DocumentHelper.createElement("EXTERNAL_ID");
    externalIdElement.addAttribute("namespace", "BioSample");
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
