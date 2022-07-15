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

import java.time.Instant;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.service.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
public class EnaElementConverter implements Converter<Element, Sample> {
  private static final String SECONDARY_ID = "SECONDARY_ID";
  private static final String COMMON_NAME = "COMMON_NAME";
  private static final String ORGANISM = "organism";
  private static final String TAG_SAMPLE_ATTRIBUTE = "attribute";
  // Fields required by ENA content - some are for JSON building and some for
  // equality checks with ENA XML
  private static final String UUID_JSON = "uuid";
  private static final String INDIVIDUAL_NAME_JSON = "individual_name";
  private static final String ANONYMIZED_NAME_JSON = "anonymized_name";
  private static final String BIOSAMPLE = "BioSample";
  private static final String NAMESPACE = "namespace";
  private static final String NAMESPACE_TAG = "Namespace:";
  private static final String SUBMITTER_ID_JSON = "Submitter Id";
  private static final String EXTERNAL_ID_JSON = "External Id";
  private static final String SECONDARY_ID_JSON = "Secondary Id";
  private static final String ALIAS = "alias";
  private static final String ENA_SRA_ACCESSION = "SRA accession";
  private static final String ENA_BROKER_NAME = "broker name";
  private static final String INSDC_CENTER_NAME = "INSDC center name";
  private static final String INSDC_CENTER_ALIAS = "INSDC center alias";
  private static final String ENA_TITLE = "title";
  private static final String ENA_DESCRIPTION = "description";
  private static final String SAMPLE = "SAMPLE";
  private static final String IDENTIFIERS = "IDENTIFIERS";
  private static final String PRIMARY_ID = "PRIMARY_ID";
  private static final String SUBMITTER_ID = "SUBMITTER_ID";
  private static final String EXTERNAL_ID = "EXTERNAL_ID";
  private static final String UUID = "UUID";
  private static final String SAMPLE_NAME = "SAMPLE_NAME";
  private static final String ANONYMIZED_NAME = "ANONYMIZED_NAME";
  private static final String INDIVIDUAL_NAME = "INDIVIDUAL_NAME";
  private static final String SCIENTIFIC_NAME = "SCIENTIFIC_NAME";
  private static final String TAXON_ID = "TAXON_ID";
  private static final String SAMPLE_ATTRIBUTE = "SAMPLE_ATTRIBUTE";
  private static final String SAMPLE_ATTRIBUTES = "SAMPLE_ATTRIBUTES";
  private static final String DESCRIPTION = "DESCRIPTION";
  private static final String TITLE = "TITLE";
  private static final String COMMON_NAME_JSON = "common name";
  private static final String PUBMED_ID = "pubmed_id";
  private Logger log = LoggerFactory.getLogger(getClass());

  @Autowired private TaxonomyService taxonomyService;

  @Override
  public Sample convert(final Element root) {
    final SortedSet<Attribute> attributes = new TreeSet<>();
    final SortedSet<Publication> publications = new TreeSet<>();
    final SortedSet<Relationship> relationships = new TreeSet<>();
    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    final Attribute organismAttribute;
    String name;
    String accession = null;

    // ENA Specific fields

    // ENA name - BSD-1741 - Requirement#1 Map name (top-attribute) in BioSamples to
    // alias (top-attribute) in ENA XML
    final String primaryId = XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, PRIMARY_ID).text();

    if (XmlPathBuilder.of(root).path(SAMPLE).attributeExists(ALIAS)) {
      name = XmlPathBuilder.of(root).path(SAMPLE).attribute(ALIAS).trim();
    } else {
      // if and only if alias is not present, then name would be equal to primaryId
      name = primaryId;
    }

    log.trace("Converting ENA sample with PRIMARY_ID as " + primaryId);

    // ENA sra accession
    if (XmlPathBuilder.of(root).path(SAMPLE).attributeExists("accession")) {
      String sraAccession = XmlPathBuilder.of(root).path(SAMPLE).attribute("accession").trim();
      attributes.add(Attribute.build(ENA_SRA_ACCESSION, sraAccession));
    }

    // ENA broker name
    if (XmlPathBuilder.of(root).path(SAMPLE).attributeExists("broker_name")) {
      String brokerName = XmlPathBuilder.of(root).path(SAMPLE).attribute("broker_name").trim();
      attributes.add(Attribute.build(ENA_BROKER_NAME, brokerName));
    }

    // ENA center name
    if (XmlPathBuilder.of(root).path(SAMPLE).attributeExists("center_name")) {
      String centerName = XmlPathBuilder.of(root).path(SAMPLE).attribute("center_name").trim();
      attributes.add(Attribute.build(INSDC_CENTER_NAME, centerName));
    }

    // ENA center alias
    if (XmlPathBuilder.of(root).path(SAMPLE).attributeExists("center_alias")) {
      String centerAlias = XmlPathBuilder.of(root).path(SAMPLE).attribute("center_alias").trim();
      attributes.add(Attribute.build(INSDC_CENTER_ALIAS, centerAlias));
    }

    // ENA title
    String title = "";
    if (XmlPathBuilder.of(root).path(SAMPLE, TITLE).exists()
        && XmlPathBuilder.of(root).path(SAMPLE, TITLE).text().trim().length() > 0) {
      title = XmlPathBuilder.of(root).path(SAMPLE, TITLE).text().trim();
    } else if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).exists()
        && XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).text().trim().length()
            > 0) {
      title = XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).text().trim();
    }
    attributes.add(Attribute.build(ENA_TITLE, title));

    // ENA description - BSD-1744 - Deal with multiple descriptions in ENA XML
    final XmlPathBuilder descriptionPathBuilder = XmlPathBuilder.of(root).path(SAMPLE, DESCRIPTION);

    if (descriptionPathBuilder.exists() && descriptionPathBuilder.text().trim().length() > 0) {
      final String description = descriptionPathBuilder.text().trim();

      attributes.add(Attribute.build(ENA_DESCRIPTION, description));
    }

    // ENA SUBMITTER_ID - BSD-1743 - Un-tag core attributes and sample attributes from synonyms
    final XmlPathBuilder submitterIdPathBuilder =
        XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, SUBMITTER_ID);
    String namespaceOfSubmitterId = null;

    if (submitterIdPathBuilder != null && submitterIdPathBuilder.attributeExists(NAMESPACE)) {
      namespaceOfSubmitterId = submitterIdPathBuilder.attribute(NAMESPACE);
    }

    if (submitterIdPathBuilder.exists()) {
      attributes.add(
          Attribute.build(
              SUBMITTER_ID_JSON,
              submitterIdPathBuilder.text(),
              namespaceOfSubmitterId != null ? NAMESPACE_TAG + namespaceOfSubmitterId : null,
              Collections.emptyList(),
              null));
    }

    // ENA EXTERNAL_ID - BSD-1743 - Un-tag core attributes and sample attributes
    // from synonyms
    accession = getString(root, attributes, accession, EXTERNAL_ID, EXTERNAL_ID_JSON);

    // ENA SECONDARY_ID
    accession = getString(root, attributes, accession, SECONDARY_ID, SECONDARY_ID_JSON);

    // ENA ANONYMIZED_NAME - BSD-1743 - Un-tag core attributes and sample attributes
    // from synonyms
    final XmlPathBuilder anonymizedNamePathBuilder =
        XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, ANONYMIZED_NAME);

    if (anonymizedNamePathBuilder.exists()) {
      attributes.add(Attribute.build(ANONYMIZED_NAME_JSON, anonymizedNamePathBuilder.text()));
    }

    // ENA INDIVIDUAL_NAME - BSD-1743 - Un-tag core attributes and sample attributes
    // from synonyms
    final XmlPathBuilder individualNamePathBuider =
        XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, INDIVIDUAL_NAME);

    if (individualNamePathBuider.exists()) {
      attributes.add(Attribute.build(INDIVIDUAL_NAME_JSON, individualNamePathBuider.text()));
    }

    // ENA UUID - BSD-1743 - Un-tag core attributes and sample attributes from
    // synonyms
    if (XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, UUID).exists()) {
      for (Element element : XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS).elements(UUID)) {
        attributes.add(Attribute.build(UUID_JSON, element.getTextTrim()));
      }
    }

    // Do the organism attribute
    int organismTaxId =
        Integer.parseInt(XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, TAXON_ID).text());
    String organismUri = taxonomyService.getUriForTaxonId(organismTaxId);
    String organismName = "" + organismTaxId;

    if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).exists()) {
      organismName = XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).text();
    }
    // ideally this should be lowercase, but backwards compatibilty...
    organismAttribute = Attribute.build(ORGANISM, organismName, organismUri, null);

    attributes.add(organismAttribute);

    if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, COMMON_NAME).exists()) {
      final String commonName =
          XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, COMMON_NAME).text();
      attributes.add(Attribute.build(COMMON_NAME_JSON, commonName));
    }

    if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_ATTRIBUTES).exists()) {
      for (Element e :
          XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_ATTRIBUTES).elements(SAMPLE_ATTRIBUTE)) {
        String tag = null;

        if (XmlPathBuilder.of(e).path("TAG").exists()
            && XmlPathBuilder.of(e).path("TAG").text().trim().length() > 0) {
          tag = XmlPathBuilder.of(e).path("TAG").text().trim();
        }

        String value = null;

        if (XmlPathBuilder.of(e).path("VALUE").exists()
            && XmlPathBuilder.of(e).path("VALUE").text().trim().length() > 0) {
          value = XmlPathBuilder.of(e).path("VALUE").text().trim();
        }

        String unit = null;

        if (XmlPathBuilder.of(e).path("UNITS").exists()
            && XmlPathBuilder.of(e).path("UNITS").text().trim().length() > 0) {
          unit = XmlPathBuilder.of(e).path("UNITS").text().trim();
        }

        // TODO handle relationships

        // BSD-1744 - Deal with multiple descriptions in ENA XML
        if (tag != null && tag.equalsIgnoreCase(ENA_DESCRIPTION)) {
          attributes.add(
              Attribute.build(tag, value, TAG_SAMPLE_ATTRIBUTE, Collections.emptyList(), null));
          continue;
        }

        // BSD-1813 - Deal with multiple titles in ENA XML
        if (tag != null && tag.equalsIgnoreCase(ENA_TITLE)) {
          attributes.add(
              Attribute.build(tag, value, TAG_SAMPLE_ATTRIBUTE, Collections.emptyList(), null));
          continue;
        }

        if (tag != null && tag.equalsIgnoreCase(PUBMED_ID)) {
          publications.add(new Publication.Builder().pubmed_id(value).build());
          continue;
        }

        if (tag != null) {
          attributes.add(
              Attribute.build(tag, value, TAG_SAMPLE_ATTRIBUTE, Collections.emptyList(), unit));
        }
      }
    }

    if (XmlPathBuilder.of(root).path(SAMPLE, "SAMPLE_LINKS").exists()) {
      if (XmlPathBuilder.of(root).path(SAMPLE, "SAMPLE_LINKS", "URI_LINK").exists()) {
        for (Element e :
            XmlPathBuilder.of(root).path(SAMPLE, "SAMPLE_LINKS").elements("URI_LINK")) {
          String key = XmlPathBuilder.of(e).attribute("LABEL");
          String value = XmlPathBuilder.of(e).attribute("URL");
          attributes.add(Attribute.build(key, value));
        }
      }

      if (XmlPathBuilder.of(root).path(SAMPLE, "SAMPLE_LINKS", "XREF_LINK").exists()) {
        for (Element e :
            XmlPathBuilder.of(root).path(SAMPLE, "SAMPLE_LINKS").elements("XREF_LINK")) {
          String key = XmlPathBuilder.of(e).attribute("DB");
          String value = XmlPathBuilder.of(e).attribute("ID");

          if (key != null && key.equalsIgnoreCase(PUBMED_ID)) {
            publications.add(new Publication.Builder().pubmed_id(value).build());
          }
        }
      }
    }

    return new Sample.Builder(name, accession)
        .withRelease(Instant.now())
        .withUpdate(Instant.now())
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withPublications(publications)
        .withExternalReferences(externalReferences)
        .build();
  }

  private String getString(
      Element root,
      SortedSet<Attribute> attributes,
      String accession,
      String externalId,
      String externalIdJson) {
    final XmlPathBuilder idPathBuilder =
        XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, externalId);

    if (idPathBuilder.exists()) {
      for (final Element element :
          XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS).elements(externalId)) {
        final String externalIdElement = XmlPathBuilder.of(element).text();

        attributes.add(
            Attribute.build(
                externalIdJson,
                externalIdElement,
                NAMESPACE_TAG + idPathBuilder.attribute(NAMESPACE),
                Collections.emptyList(),
                null));

        if (BIOSAMPLE.equals(element.attributeValue(NAMESPACE))) {
          accession = externalIdElement;
        }
      }
    }

    return accession;
  }
}
