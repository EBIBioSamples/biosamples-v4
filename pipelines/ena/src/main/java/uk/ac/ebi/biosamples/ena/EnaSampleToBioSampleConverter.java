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

import static uk.ac.ebi.biosamples.ena.EnaSampleToBioSampleConversionConstants.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.dom4j.Element;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
class EnaSampleToBioSampleConverter {
  private static final String ENA_SAMPLE_ACCESSION = "accession";

  Sample convertEnaSampleXmlToBioSample(
      final Element eraSampleXmlRootElement,
      final String bioSampleAccession,
      final boolean isNcbi) {
    final SortedSet<Attribute> bioSampleAttributes = new TreeSet<>();
    final SortedSet<Publication> bioSamplePublications = new TreeSet<>();
    final SortedSet<Relationship> bioSampleRelationships = new TreeSet<>();
    final SortedSet<ExternalReference> bioSampleExternalReferences = new TreeSet<>();
    final XmlPathBuilder eraSampleXmlRootPath =
        XmlPathBuilder.of(eraSampleXmlRootElement).path(ENA_SAMPLE_ROOT);

    /*Map name (top-attribute) in BioSamples to
    alias (top-attribute) in ENA XML*/
    String bioSampleSampleName = null;
    String bioSampleTaxId = null;

    if (eraSampleXmlRootPath.attributeExists(ENA_SAMPLE_ALIAS)) {
      bioSampleSampleName = eraSampleXmlRootPath.attribute(ENA_SAMPLE_ALIAS).trim();
    }

    // ENA SRA accession
    if (eraSampleXmlRootPath.attributeExists(ENA_SAMPLE_ACCESSION)) {
      final String enaSampleSraAccession =
          eraSampleXmlRootPath.attribute(ENA_SAMPLE_ACCESSION).trim();
      bioSampleAttributes.add(Attribute.build(ENA_SAMPLE_SRA_ACCESSION, enaSampleSraAccession));

      /*if and only if alias is not present, then sample name in BioSamples would be equal to
      ENA sample accession*/
      if (bioSampleSampleName == null) {
        bioSampleSampleName = enaSampleSraAccession;
      }
    }

    // ENA broker name
    if (eraSampleXmlRootPath.attributeExists("broker_name")) {
      bioSampleAttributes.add(
          Attribute.build(
              ENA_SAMPLE_BROKER_NAME, eraSampleXmlRootPath.attribute("broker_name").trim()));
    }

    // ENA center name
    if (eraSampleXmlRootPath.attributeExists("center_name")) {
      bioSampleAttributes.add(
          Attribute.build(
              BIOSAMPLE_INSDC_CENTER_NAME_ATTRIBUTE_NAME,
              eraSampleXmlRootPath.attribute("center_name").trim()));
    }

    // ENA center alias
    if (eraSampleXmlRootPath.attributeExists("center_alias")) {
      bioSampleAttributes.add(
          Attribute.build(
              BIOSAMPLE_INSDC_CENTER_ALIAS_ATTRIBUTE_NAME,
              eraSampleXmlRootPath.attribute("center_alias").trim()));
    }

    // ENA title
    final XmlPathBuilder titlePathBuilder =
        XmlPathBuilder.of(eraSampleXmlRootElement).path(ENA_SAMPLE_ROOT, ENA_SAMPLE_TITLE);

    if (titlePathBuilder.exists()) {
      bioSampleAttributes.add(
          Attribute.build(BIOSAMPLE_SAMPLE_TITLE_ATTRIBUTE_NAME, titlePathBuilder.text().trim()));
    }

    // ENA description
    final XmlPathBuilder descriptionPathBuilder =
        XmlPathBuilder.of(eraSampleXmlRootElement).path(ENA_SAMPLE_ROOT, ENA_SAMPLE_DESCRIPTION);

    if (descriptionPathBuilder.exists()) {
      bioSampleAttributes.add(
          Attribute.build(
              BIOSAMPLE_SAMPLE_DESCRIPTION_ATTRIBUTE_NAME, descriptionPathBuilder.text().trim()));
    }

    // ENA SUBMITTER_ID
    final XmlPathBuilder submitterIdPathBuilder =
        XmlPathBuilder.of(eraSampleXmlRootElement)
            .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_DENTIFIERS, ENA_SAMPLE_SUBMITTER_ID);
    String namespaceOfSubmitterId = null;

    if (submitterIdPathBuilder != null && submitterIdPathBuilder.exists()) {
      if (submitterIdPathBuilder.attributeExists(ENA_IDENTIFIERS_NAMESPACE_TAG)) {
        namespaceOfSubmitterId = submitterIdPathBuilder.attribute(ENA_IDENTIFIERS_NAMESPACE_TAG);
      }

      bioSampleAttributes.add(
          Attribute.build(
              BIOSAMPLE_SUBMITTER_ID_ATTRIBUTE_NAME,
              submitterIdPathBuilder.text(),
              namespaceOfSubmitterId != null
                  ? BIOSAMPLE_ATTRIBUTE_NAMESPACE_TAG + namespaceOfSubmitterId
                  : null,
              Collections.emptyList(),
              null));
    }

    // ENA EXTERNAL ID
    final XmlPathBuilder externalIdPathBuilder =
        XmlPathBuilder.of(eraSampleXmlRootElement)
            .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_DENTIFIERS, ENA_SAMPLE_EXTERNAL_ID);
    final List<Element> externalIdPathBuilderElements =
        XmlPathBuilder.of(eraSampleXmlRootElement)
            .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_DENTIFIERS)
            .elements(ENA_SAMPLE_EXTERNAL_ID);

    for (final Element element : externalIdPathBuilderElements) {
      String namespaceOfExternalId = null;
      final XmlPathBuilder singleExternalIdPathBuilder = XmlPathBuilder.of(element);

      if (singleExternalIdPathBuilder.exists()) {
        if (singleExternalIdPathBuilder.attributeExists(ENA_IDENTIFIERS_NAMESPACE_TAG)) {
          namespaceOfExternalId =
              singleExternalIdPathBuilder.attribute(ENA_IDENTIFIERS_NAMESPACE_TAG);
        }

        bioSampleAttributes.add(
            Attribute.build(
                BIOSAMPLE_EXTERNAL_ID_ATTRIBUTE_NAME,
                singleExternalIdPathBuilder.text(),
                namespaceOfExternalId != null
                    ? BIOSAMPLE_ATTRIBUTE_NAMESPACE_TAG
                        + externalIdPathBuilder.attribute(ENA_IDENTIFIERS_NAMESPACE_TAG)
                    : null,
                Collections.emptyList(),
                null));
      }
    }

    // ENA SECONDARY ID
    final XmlPathBuilder secondaryIdPathBuilder =
        XmlPathBuilder.of(eraSampleXmlRootElement)
            .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_DENTIFIERS, ENA_SAMPLE_SECONDARY_ID);

    if (secondaryIdPathBuilder.exists()) {
      final List<Element> secondaryIdPathBuilderElements =
          XmlPathBuilder.of(eraSampleXmlRootElement)
              .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_DENTIFIERS)
              .elements(ENA_SAMPLE_SECONDARY_ID);

      for (final Element element : secondaryIdPathBuilderElements) {
        final XmlPathBuilder singleSecondaryIdPathBuilder = XmlPathBuilder.of(element);

        if (singleSecondaryIdPathBuilder.exists()) {
          String namespaceOfExternalId = null;

          if (singleSecondaryIdPathBuilder.attributeExists(ENA_IDENTIFIERS_NAMESPACE_TAG)) {
            namespaceOfExternalId =
                singleSecondaryIdPathBuilder.attribute(ENA_IDENTIFIERS_NAMESPACE_TAG);
          }

          bioSampleAttributes.add(
              Attribute.build(
                  BIOSAMPLE_SECONDARY_ID_ATTRIBUTE_NAME,
                  singleSecondaryIdPathBuilder.text(),
                  namespaceOfExternalId != null
                      ? BIOSAMPLE_ATTRIBUTE_NAMESPACE_TAG
                          + secondaryIdPathBuilder.attribute(ENA_IDENTIFIERS_NAMESPACE_TAG)
                      : null,
                  Collections.emptyList(),
                  null));
        }
      }
    }

    // ENA UUID
    if (XmlPathBuilder.of(eraSampleXmlRootElement)
        .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_DENTIFIERS, ENA_SAMPLE_UUID)
        .exists()) {
      for (final Element element :
          XmlPathBuilder.of(eraSampleXmlRootElement)
              .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_DENTIFIERS)
              .elements(ENA_SAMPLE_UUID)) {
        bioSampleAttributes.add(
            Attribute.build(BIOSAMPLE_UUID_ATTRIBUTE_NAME, element.getTextTrim()));
      }
    }

    // ENA ANONYMIZED_NAME
    final XmlPathBuilder anonymizedNamePathBuilder =
        XmlPathBuilder.of(eraSampleXmlRootElement)
            .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_SAMPLE_NAME, ENA_SAMPLE_ANONYMIZED_NAME);

    if (anonymizedNamePathBuilder.exists()) {
      bioSampleAttributes.add(
          Attribute.build(
              BIOSAMPLE_ANONYMIZED_NAME_ATTRIBUTE_NAME, anonymizedNamePathBuilder.text()));
    }

    // ENA INDIVIDUAL_NAME
    final XmlPathBuilder individualNamePathBuilder =
        XmlPathBuilder.of(eraSampleXmlRootElement)
            .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_SAMPLE_NAME, ENA_SAMPLE_INDIVIDUAL_NAME);

    if (individualNamePathBuilder.exists()) {
      bioSampleAttributes.add(
          Attribute.build(
              BIOSAMPLE_INDIVIDUAL_NAME_ATTRIBUTE_NAME, individualNamePathBuilder.text()));
    }

    // Handle organism attribute
    final XmlPathBuilder scientificNamePathBuilder =
        XmlPathBuilder.of(eraSampleXmlRootElement)
            .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_SAMPLE_NAME, ENA_SAMPLE_SCIENTIFIC_NAME);

    if (scientificNamePathBuilder.exists()) {
      bioSampleAttributes.add(
          Attribute.build(
              BIOSAMPLE_ORGANISM_ATTRIBUTE_NAME, scientificNamePathBuilder.text(), null, null));
      bioSampleAttributes.add(
          Attribute.build(
              BIOSAMPLE_SCIENTIFIC_NAME_ATTRIBUTE_NAME,
              scientificNamePathBuilder.text(),
              null,
              null));
    }

    // Handle TAXON_ID
    final XmlPathBuilder taxonIdPathBuilder =
        XmlPathBuilder.of(eraSampleXmlRootElement)
            .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_SAMPLE_NAME, "TAXON_ID");

    if (taxonIdPathBuilder.exists()) {
      bioSampleTaxId = taxonIdPathBuilder.text();
    }

    // ENA sample common name
    final XmlPathBuilder commonNamePathBuilder =
        XmlPathBuilder.of(eraSampleXmlRootElement)
            .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_SAMPLE_NAME, ENA_SAMPLE_COMMON_NAME);

    if (commonNamePathBuilder.exists()) {
      bioSampleAttributes.add(
          Attribute.build(BIOSAMPLE_COMMON_NAME_ATTRIBUTE_NAME, commonNamePathBuilder.text()));
    }

    final XmlPathBuilder sampleAttributesPathBuilder =
        XmlPathBuilder.of(eraSampleXmlRootElement)
            .path(ENA_SAMPLE_ROOT, ENA_SAMPLE_SAMPLE_ATTRIBUTES);

    if (sampleAttributesPathBuilder.exists()) {
      for (final Element attributeElement :
          sampleAttributesPathBuilder.elements(ENA_SAMPLE_SAMPLE_ATTRIBUTE)) {
        String tag = null;
        final XmlPathBuilder tagPathBuilder = XmlPathBuilder.of(attributeElement).path("TAG");

        if (tagPathBuilder.exists() && tagPathBuilder.text().trim().length() > 0) {
          tag = tagPathBuilder.text().trim();
        }

        String value = null;
        final XmlPathBuilder valuePathBuilder = XmlPathBuilder.of(attributeElement).path("VALUE");

        if (valuePathBuilder.exists() && valuePathBuilder.text().trim().length() > 0) {
          value = valuePathBuilder.text().trim();
        }

        String unit = null;
        final XmlPathBuilder unitPathBuilder = XmlPathBuilder.of(attributeElement).path("UNITS");

        if (unitPathBuilder.exists() && unitPathBuilder.text().trim().length() > 0) {
          unit = unitPathBuilder.text().trim();
        }

        // Deal with multiple descriptions in ENA XML, one top level description and one in sample
        // attributes
        if (tag != null && tag.equalsIgnoreCase(BIOSAMPLE_SAMPLE_DESCRIPTION_ATTRIBUTE_NAME)) {
          bioSampleAttributes.add(
              Attribute.build(
                  tag, value, BIOSAMPLE_SAMPLE_ATTRIBUTE_TAG, Collections.emptyList(), null));
          continue;
        }

        // Deal with multiple titles in ENA XML, one top level title and one in sample attributes
        if (tag != null && tag.equalsIgnoreCase(BIOSAMPLE_SAMPLE_TITLE_ATTRIBUTE_NAME)) {
          bioSampleAttributes.add(
              Attribute.build(
                  tag, value, BIOSAMPLE_SAMPLE_ATTRIBUTE_TAG, Collections.emptyList(), null));
          continue;
        }

        if (tag != null && tag.equalsIgnoreCase(ENA_SAMPLE_PUBMED_ID)) {
          bioSamplePublications.add(new Publication.Builder().pubmed_id(value).build());
          continue;
        }

        if (tag != null) {
          bioSampleAttributes.add(
              Attribute.build(
                  tag, value, BIOSAMPLE_SAMPLE_ATTRIBUTE_TAG, Collections.emptyList(), unit));
        }
      }
    }

    if (XmlPathBuilder.of(eraSampleXmlRootElement).path(ENA_SAMPLE_ROOT, "SAMPLE_LINKS").exists()) {
      if (XmlPathBuilder.of(eraSampleXmlRootElement)
          .path(ENA_SAMPLE_ROOT, "SAMPLE_LINKS", "URI_LINK")
          .exists()) {
        for (final Element e :
            XmlPathBuilder.of(eraSampleXmlRootElement)
                .path(ENA_SAMPLE_ROOT, "SAMPLE_LINKS")
                .elements("URI_LINK")) {
          bioSampleAttributes.add(
              Attribute.build(
                  XmlPathBuilder.of(e).attribute("LABEL"), XmlPathBuilder.of(e).attribute("URL")));
        }
      }

      if (XmlPathBuilder.of(eraSampleXmlRootElement)
          .path(ENA_SAMPLE_ROOT, "SAMPLE_LINKS", "XREF_LINK")
          .exists()) {
        for (final Element e :
            XmlPathBuilder.of(eraSampleXmlRootElement)
                .path(ENA_SAMPLE_ROOT, "SAMPLE_LINKS")
                .elements("XREF_LINK")) {
          final String key = XmlPathBuilder.of(e).attribute("DB");

          if (key != null && key.equalsIgnoreCase(ENA_SAMPLE_PUBMED_ID)) {
            bioSamplePublications.add(
                new Publication.Builder().pubmed_id(XmlPathBuilder.of(e).attribute("ID")).build());
          }
        }
      }
    }

    return new Sample.Builder(bioSampleSampleName, bioSampleAccession)
        .withTaxId(bioSampleTaxId != null ? Long.valueOf(bioSampleTaxId) : null)
        .withRelease(Instant.now())
        .withUpdate(Instant.now())
        .withAttributes(bioSampleAttributes)
        .withRelationships(bioSampleRelationships)
        .withPublications(bioSamplePublications)
        .withExternalReferences(bioSampleExternalReferences)
        .build();
  }
}
