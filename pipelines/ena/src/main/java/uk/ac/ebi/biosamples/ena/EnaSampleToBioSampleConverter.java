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
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.service.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
public class EnaSampleToBioSampleConverter {
  private static final String SECONDARY_ID = "SECONDARY_ID";
  private static final String COMMON_NAME = "COMMON_NAME";
  private static final String ORGANISM = "organism";
  private static final String TAG_SAMPLE_ATTRIBUTE = "attribute";
  // Fields required by ENA content - some are for JSON building and some for
  // equality checks with ENA XML
  private static final String UUID_JSON = "uuid";
  private static final String INDIVIDUAL_NAME_JSON = "individual_name";
  private static final String ANONYMIZED_NAME_JSON = "anonymized_name";
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
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Autowired private TaxonomyService taxonomyService;

  public Sample convert(final Element enaSampleRootElement, final String accession) {
    final SortedSet<Attribute> attributes = new TreeSet<>();
    final SortedSet<Publication> publications = new TreeSet<>();
    final SortedSet<Relationship> relationships = new TreeSet<>();
    final Attribute organismAttribute;
    String enaSampleName;

    // ENA Specific fields

    // ENA enaSampleName - BSD-1741 - Requirement#1 Map enaSampleName (top-attribute) in BioSamples
    // to
    // alias (top-attribute) in ENA XML
    final String primaryId =
        XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, IDENTIFIERS, PRIMARY_ID).text();
    final XmlPathBuilder sampleTagPathBuilder =
        XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE);

    if (sampleTagPathBuilder.attributeExists(ALIAS)) {
      enaSampleName = sampleTagPathBuilder.attribute(ALIAS).trim();
    } else {
      // if and only if alias is not present, then enaSampleName would be equal to primaryId
      enaSampleName = primaryId;
    }

    log.trace("Converting ENA sample with PRIMARY_ID as " + primaryId);

    // ENA SRA accession
    if (sampleTagPathBuilder.attributeExists("accession")) {
      attributes.add(
          Attribute.build(ENA_SRA_ACCESSION, sampleTagPathBuilder.attribute("accession").trim()));
    }

    // ENA broker enaSampleName
    if (sampleTagPathBuilder.attributeExists("broker_name")) {
      attributes.add(
          Attribute.build(ENA_BROKER_NAME, sampleTagPathBuilder.attribute("broker_name").trim()));
    }

    // ENA center enaSampleName
    if (sampleTagPathBuilder.attributeExists("center_name")) {
      attributes.add(
          Attribute.build(INSDC_CENTER_NAME, sampleTagPathBuilder.attribute("center_name").trim()));
    }

    // ENA center alias
    if (sampleTagPathBuilder.attributeExists("center_alias")) {
      attributes.add(
          Attribute.build(
              INSDC_CENTER_ALIAS, sampleTagPathBuilder.attribute("center_alias").trim()));
    }

    // ENA title
    final XmlPathBuilder sampleTitlePathBuilder =
        XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, TITLE);
    final XmlPathBuilder scientificNamePathBuilder =
        XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME);

    String title = "";

    if (sampleTitlePathBuilder.exists() && sampleTitlePathBuilder.text().trim().length() > 0) {
      title = sampleTitlePathBuilder.text().trim();
    } else if (scientificNamePathBuilder.exists()
        && scientificNamePathBuilder.text().trim().length() > 0) {
      title = scientificNamePathBuilder.text().trim();
    }

    if (!title.isEmpty()) {
      attributes.add(Attribute.build(ENA_TITLE, title));

      enaSampleName = title;
    }

    // ENA description - BSD-1744 - Deal with multiple descriptions in ENA XML
    final XmlPathBuilder descriptionPathBuilder =
        XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, DESCRIPTION);

    if (descriptionPathBuilder.exists() && descriptionPathBuilder.text().trim().length() > 0) {
      attributes.add(Attribute.build(ENA_DESCRIPTION, descriptionPathBuilder.text().trim()));
    }

    // ENA SUBMITTER_ID - BSD-1743 - Un-tag core attributes and sample attributes from synonyms
    parseEnaIdentifiersWithNamespaces(
        enaSampleRootElement, attributes, SUBMITTER_ID, SUBMITTER_ID_JSON);

    // ENA EXTERNAL_ID - BSD-1743 - Un-tag core attributes and sample attributes from synonyms
    parseEnaIdentifiersWithNamespaces(
        enaSampleRootElement, attributes, EXTERNAL_ID, EXTERNAL_ID_JSON);

    // ENA SECONDARY_ID
    parseEnaIdentifiersWithNamespaces(
        enaSampleRootElement, attributes, SECONDARY_ID, SECONDARY_ID_JSON);

    // ENA ANONYMIZED_NAME - BSD-1743 - Un-tag core attributes and sample attributes from synonyms
    final XmlPathBuilder anonymizedNamePathBuilder =
        XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, SAMPLE_NAME, ANONYMIZED_NAME);

    if (anonymizedNamePathBuilder.exists()) {
      attributes.add(Attribute.build(ANONYMIZED_NAME_JSON, anonymizedNamePathBuilder.text()));
    }

    // ENA INDIVIDUAL_NAME - BSD-1743 - Un-tag core attributes and sample attributes from synonyms
    final XmlPathBuilder individualNamePathBuider =
        XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, SAMPLE_NAME, INDIVIDUAL_NAME);

    if (individualNamePathBuider.exists()) {
      attributes.add(Attribute.build(INDIVIDUAL_NAME_JSON, individualNamePathBuider.text()));
    }

    // ENA UUID - BSD-1743 - Un-tag core attributes and sample attributes from synonyms
    if (XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, IDENTIFIERS, UUID).exists()) {
      for (final Element element :
          XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, IDENTIFIERS).elements(UUID)) {
        attributes.add(Attribute.build(UUID_JSON, element.getTextTrim()));
      }
    }

    // Handle the organism attribute
    final int organismTaxId =
        Integer.parseInt(
            XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, SAMPLE_NAME, TAXON_ID).text());
    final String organismUri = taxonomyService.getUriForTaxonId(organismTaxId);
    String organismName = String.valueOf(taxonomyService.getUriForTaxonId(organismTaxId));

    if (scientificNamePathBuilder.exists()) {
      organismName = scientificNamePathBuilder.text();
    }
    // ideally this should be lowercase, but backwards compatibility
    organismAttribute = Attribute.build(ORGANISM, organismName, organismUri, null);

    attributes.add(organismAttribute);

    final XmlPathBuilder commonNamePathBuilder =
        XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, SAMPLE_NAME, COMMON_NAME);

    if (commonNamePathBuilder.exists()) {
      attributes.add(Attribute.build(COMMON_NAME_JSON, commonNamePathBuilder.text()));
    }

    final XmlPathBuilder sampleAttributesPathBuilder =
        XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, SAMPLE_ATTRIBUTES);

    if (sampleAttributesPathBuilder.exists()) {
      for (final Element sampleAttributeElement :
          sampleAttributesPathBuilder.elements(SAMPLE_ATTRIBUTE)) {
        final XmlPathBuilder enaAttributeTagPathBuilder =
            XmlPathBuilder.of(sampleAttributeElement).path("TAG");
        String tag = null;

        if (enaAttributeTagPathBuilder.exists()
            && enaAttributeTagPathBuilder.text().trim().length() > 0) {
          tag = enaAttributeTagPathBuilder.text().trim();
        }

        final XmlPathBuilder enaAttributeValuePathBuilder =
            XmlPathBuilder.of(sampleAttributeElement).path("VALUE");
        String value = null;

        if (enaAttributeValuePathBuilder.exists()
            && enaAttributeValuePathBuilder.text().trim().length() > 0) {
          value = enaAttributeValuePathBuilder.text().trim();
        }

        final XmlPathBuilder enaAttributeUnitPathBuilder =
            XmlPathBuilder.of(sampleAttributeElement).path("UNITS");
        String unit = null;

        if (enaAttributeUnitPathBuilder.exists()
            && enaAttributeUnitPathBuilder.text().trim().length() > 0) {
          unit = enaAttributeUnitPathBuilder.text().trim();
        }

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

    if (XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, "SAMPLE_LINKS").exists()) {
      if (XmlPathBuilder.of(enaSampleRootElement)
          .path(SAMPLE, "SAMPLE_LINKS", "URI_LINK")
          .exists()) {
        for (final Element sampleLinkElement :
            XmlPathBuilder.of(enaSampleRootElement)
                .path(SAMPLE, "SAMPLE_LINKS")
                .elements("URI_LINK")) {
          attributes.add(
              Attribute.build(
                  XmlPathBuilder.of(sampleLinkElement).attribute("LABEL"),
                  XmlPathBuilder.of(sampleLinkElement).attribute("URL")));
        }
      }

      if (XmlPathBuilder.of(enaSampleRootElement)
          .path(SAMPLE, "SAMPLE_LINKS", "XREF_LINK")
          .exists()) {
        for (final Element sampleLinkElement :
            XmlPathBuilder.of(enaSampleRootElement)
                .path(SAMPLE, "SAMPLE_LINKS")
                .elements("XREF_LINK")) {
          final String key = XmlPathBuilder.of(sampleLinkElement).attribute("DB");

          if (key != null && key.equalsIgnoreCase(PUBMED_ID)) {
            publications.add(
                new Publication.Builder()
                    .pubmed_id(XmlPathBuilder.of(sampleLinkElement).attribute("ID"))
                    .build());
          }
        }
      }
    }

    return new Sample.Builder(enaSampleName, accession)
        .withRelease(Instant.now())
        .withUpdate(Instant.now())
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withPublications(publications)
        .withExternalReferences(
            Collections.singletonList(
                ExternalReference.build("https://www.ebi.ac.uk/ena/browser/view/" + accession)))
        .build();
  }

  private void parseEnaIdentifiersWithNamespaces(
      final Element enaSampleRootElement,
      final SortedSet<Attribute> attributes,
      final String enaIdName,
      final String bioSampleAttributeType) {
    try {
      final XmlPathBuilder idPathBuilder =
          XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, IDENTIFIERS, enaIdName);

      if (idPathBuilder.exists()) {
        for (final Element element :
            XmlPathBuilder.of(enaSampleRootElement).path(SAMPLE, IDENTIFIERS).elements(enaIdName)) {
          final String idElement = XmlPathBuilder.of(element).text();

          attributes.add(
              Attribute.build(
                  bioSampleAttributeType,
                  idElement,
                  NAMESPACE_TAG + idPathBuilder.attribute(NAMESPACE),
                  Collections.emptyList(),
                  null));
        }
      }
    } catch (final Exception e) {
      log.info("Failed to parse PATH for " + enaIdName);
    }
  }
}
