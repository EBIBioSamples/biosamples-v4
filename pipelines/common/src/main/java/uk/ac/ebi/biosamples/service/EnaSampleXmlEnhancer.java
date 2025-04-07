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
package uk.ac.ebi.biosamples.service;

import static uk.ac.ebi.biosamples.service.EnaXmlUtil.pretty;

import java.io.StringReader;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
public class EnaSampleXmlEnhancer {
  private static final Logger LOGGER = LoggerFactory.getLogger(EnaSampleXmlEnhancer.class);

  public String applyRules(
      final String inputXml, final EraproSample eraproSample, final Rule... rules) {
    final Element element = getSampleElement(inputXml);
    final Element modifiedElement = applyRules(element.createCopy(), eraproSample, rules);
    final Document document = DocumentHelper.createDocument();

    document.setRootElement(modifiedElement);

    return pretty(document);
  }

  private Element applyRules(
      Element sampleElement, final EraproSample eraproSample, final Rule... rules) {
    for (final Rule rule : rules) {
      sampleElement = rule.apply(sampleElement, eraproSample);
    }

    return sampleElement;
  }

  public String applyAllRules(final String inputXml, final EraproSample eraproSample) {
    return applyRules(
        inputXml,
        eraproSample,
        AliasRule.INSTANCE,
        NamespaceRule.INSTANCE,
        BrokerRule.INSTANCE,
        LinkRemovalRule.INSTANCE,
        CenterNameRule.INSTANCE,
        DatesRule.INSTANCE,
        BioSamplesIdRule.INSTANCE);
  }

  public Element applyAllRules(final Element element, final EraproSample eraproSample) {
    return applyRules(
        element,
        eraproSample,
        AliasRule.INSTANCE,
        NamespaceRule.INSTANCE,
        BrokerRule.INSTANCE,
        LinkRemovalRule.INSTANCE,
        CenterNameRule.INSTANCE,
        DatesRule.INSTANCE,
        BioSamplesIdRule.INSTANCE);
  }

  public interface Rule {
    Element apply(Element sampleXml, EraproSample eraproSample);
  }

  public enum AliasRule implements Rule {
    INSTANCE;

    @Override
    public Element apply(final Element sampleXml, final EraproSample eraproSample) {
      if (!XmlPathBuilder.of(sampleXml).path("SAMPLE").attributeExists("alias")) {
        final XmlPathBuilder xmlPathBuilder =
            XmlPathBuilder.of(sampleXml).path("SAMPLE", "IDENTIFIERS", "SUBMITTER_ID");

        if (xmlPathBuilder.exists()) {
          final Node node = sampleXml.selectSingleNode("SAMPLE/IDENTIFIERS/SUBMITTER_ID");

          node.detach();
        }
      }

      return sampleXml;
    }
  }

  public enum NamespaceRule implements Rule {
    INSTANCE;

    @Override
    public Element apply(final Element sampleXml, final EraproSample eraproSample) {
      if (!XmlPathBuilder.of(sampleXml).path("SAMPLE").attributeExists("center_name")) {
        return sampleXml;
      }

      final XmlPathBuilder xmlPathBuilder =
          XmlPathBuilder.of(sampleXml).path("SAMPLE", "IDENTIFIERS", "SUBMITTER_ID");

      if (xmlPathBuilder.exists()) {
        if (!xmlPathBuilder.attributeExists("namespace")
            || xmlPathBuilder.attribute("namespace").isEmpty()) {
          final String centerName =
              XmlPathBuilder.of(sampleXml).path("SAMPLE").attribute("center_name");
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
    public Element apply(final Element sampleXml, final EraproSample eraproSample) {
      final XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE");

      if (xmlPathBuilder.attributeExists("accession")) {
        final String accession = xmlPathBuilder.attribute("accession");

        if (accession.startsWith("ERS")) {
          if (eraproSample.brokerName != null) {
            xmlPathBuilder.element().addAttribute("broker_name", eraproSample.brokerName);
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
    public Element apply(final Element sampleXml, final EraproSample eraproSample) {
      final XmlPathBuilder xmlPathBuilder =
          XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_LINKS");
      if (xmlPathBuilder.exists()) {
        for (final Element sampleLinkElement : xmlPathBuilder.elements("SAMPLE_LINK")) {
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
    public Element apply(final Element sampleXml, final EraproSample eraproSample) {
      if (eraproSample.centreName != null) {
        final XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE");

        if (xmlPathBuilder.attributeExists("center_name")) {
          xmlPathBuilder
              .element()
              .addAttribute("center_alias", xmlPathBuilder.attribute("center_name"));
          xmlPathBuilder.element().addAttribute("center_name", eraproSample.centreName);
        }
      }

      return sampleXml;
    }
  }

  public enum DatesRule implements Rule {
    INSTANCE;

    @Override
    public Element apply(final Element sampleXml, final EraproSample eraproSample) {
      if (eraproSample.firstPublic == null || eraproSample.lastUpdated == null) {
        return sampleXml;
      }

      final XmlPathBuilder xmlPathBuilder =
          XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_ATTRIBUTES");

      if (xmlPathBuilder.exists()) {
        xmlPathBuilder
            .element()
            .add(createSampleAttribute("ENA-FIRST-PUBLIC", eraproSample.firstPublic));
        xmlPathBuilder
            .element()
            .add(createSampleAttribute("ENA-LAST-UPDATE", eraproSample.lastUpdated));
      }

      return sampleXml;
    }
  }

  public enum BioSamplesIdRule implements Rule {
    INSTANCE;

    @Override
    public Element apply(final Element sampleXml, final EraproSample eraproSample) {
      if (eraproSample.biosampleId == null) {
        return sampleXml;
      }

      final XmlPathBuilder xmlPathBuilder =
          XmlPathBuilder.of(sampleXml).path("SAMPLE", "IDENTIFIERS");

      if (xmlPathBuilder.exists()) {
        boolean bioSamplesExternalIdExists = false;

        for (final Element element : xmlPathBuilder.elements("EXTERNAL_ID")) {
          if (element.attribute("namespace").getText().equals("BioSample")) {
            bioSamplesExternalIdExists = true;
          }
        }

        if (!bioSamplesExternalIdExists) {
          xmlPathBuilder.element().add(createExternalRef(eraproSample.biosampleId));
        }
      }

      return sampleXml;
    }
  }

  public enum TitleRule implements Rule {
    INSTANCE;

    @Override
    public Element apply(final Element sampleXml, final EraproSample eraproSample) {
      if (eraproSample.biosampleId == null) {
        return sampleXml;
      }

      XmlPathBuilder xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE", "TITLE");

      if (!xmlPathBuilder.exists()) {
        String newTitle = null;

        if (eraproSample.scientificName != null) {
          newTitle = eraproSample.scientificName;
        }

        if (newTitle == null && eraproSample.commonName != null) {
          newTitle = eraproSample.commonName;
        }

        if (newTitle == null && eraproSample.taxId != null) {
          newTitle = String.valueOf(eraproSample.taxId);
        }

        if (newTitle != null) {
          xmlPathBuilder = XmlPathBuilder.of(sampleXml).path("SAMPLE");

          final Element titleElement = DocumentHelper.createElement("TITLE");

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
    public Element apply(final Element sampleXml, final EraproSample eraproSample) {
      final XmlPathBuilder parentXmlPathBuilderName =
          XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_NAME");

      if (!parentXmlPathBuilderName.exists()) {
        XmlPathBuilder.of(sampleXml).path("SAMPLE").element().addElement("SAMPLE_NAME");
      }

      final XmlPathBuilder taxIdXmlPathBuilderTaxonId =
          XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_NAME", "TAXON_ID");
      final String taxId = eraproSample.taxId == null ? "" : String.valueOf(eraproSample.taxId);

      if (taxIdXmlPathBuilderTaxonId.exists()) {
        taxIdXmlPathBuilderTaxonId.element().setText(taxId);
      } else {
        parentXmlPathBuilderName.element().addElement("TAXON_ID", taxId);
      }

      final XmlPathBuilder taxIdXmlPathBuilderScientificName =
          XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_NAME", "SCIENTIFIC_NAME");
      final String scientificName =
          eraproSample.scientificName == null ? "" : eraproSample.scientificName;

      if (taxIdXmlPathBuilderScientificName.exists()) {
        taxIdXmlPathBuilderScientificName.element().setText(scientificName);
      } else {
        parentXmlPathBuilderName.element().addElement("SCIENTIFIC_NAME", scientificName);
      }

      final XmlPathBuilder taxIdXmlPathBuilderCommonName =
          XmlPathBuilder.of(sampleXml).path("SAMPLE", "SAMPLE_NAME", "COMMON_NAME");
      final String commonName = eraproSample.commonName == null ? "" : eraproSample.commonName;

      if (taxIdXmlPathBuilderCommonName.exists()) {
        taxIdXmlPathBuilderCommonName.element().setText(commonName);
      } else {
        parentXmlPathBuilderName.element().addElement("COMMON_NAME", commonName);
      }

      return sampleXml;
    }
  }

  private Element getSampleElement(final String xmlString) {
    final SAXReader reader = new SAXReader();
    Document xml = null;

    try {
      xml = reader.read(new StringReader(xmlString));
    } catch (final DocumentException e) {
      LOGGER.error("Error reading XML", e);
    }

    assert xml != null;

    return xml.getRootElement();
  }

  private static Element createExternalRef(final String bioSamplesId) {
    final Element externalIdElement = DocumentHelper.createElement("EXTERNAL_ID");

    externalIdElement.addAttribute("namespace", "BioSample");
    externalIdElement.setText(bioSamplesId);

    return externalIdElement;
  }

  private static Element createSampleAttribute(final String tag, final String value) {
    final Element sampleAttributeElement = DocumentHelper.createElement("SAMPLE_ATTRIBUTE");
    final Element tagElement = DocumentHelper.createElement("TAG");

    tagElement.setText(tag);
    sampleAttributeElement.add(tagElement);

    final Element valueElement = DocumentHelper.createElement("VALUE");
    valueElement.setText(value);
    sampleAttributeElement.add(valueElement);

    return sampleAttributeElement;
  }
}
