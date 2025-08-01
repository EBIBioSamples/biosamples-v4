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
package uk.ac.ebi.biosamples.ncbi.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.*;
import uk.ac.ebi.biosamples.core.model.structured.StructuredDataEntry;
import uk.ac.ebi.biosamples.core.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.ncbi.service.NcbiAmrConversionService.AmrParsingException;
import uk.ac.ebi.biosamples.service.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
public class NcbiSampleConversionService {
  private static final Logger log = LoggerFactory.getLogger(NcbiSampleConversionService.class);
  private static final String SUBMISSION_DATE = "submission_date";
  private static final String COMMON_NAME = "common name";
  private static final String GENBANK = "GenBank";
  private static final String SUPPRESSED = "suppressed";
  private static final String LIVE = "live";
  private static final String INSDC_STATUS = "INSDC status";
  private static final String STATUS_LOWER_CASE = "status";
  private static final String STATUS = "Status";
  private static final String INSDC_LAST_UPDATE = "INSDC last update";
  private static final String INSDC_FIRST_PUBLIC = "INSDC first public";
  private static final String PUBLICATION_DATE = "publication_date";
  private static final String Z = "Z";
  private static final String LAST_UPDATE = "last_update";
  private static final String PACKAGE = "Package";
  private static final String NCBI_SUBMISSION_PACKAGE = "NCBI submission package";
  private static final String NCBI_SUBMISSION_MODEL = "NCBI submission model";
  private static final String MODEL = "Model";
  private static final String MODELS = "Models";
  private static final String ATTRIBUTE_NAME = "attribute_name";
  private static final String DISPLAY_NAME = "display_name";
  private static final String ATTRIBUTE = "Attribute";
  private static final String ATTRIBUTES = "Attributes";
  private static final String LINKS = "Links";
  private static final String LINK = "Link";
  private static final String TAXONOMY_NAME = "taxonomy_name";
  private static final String TAXONOMY_ID = "taxonomy_id";
  private static final String ORGANISM = "Organism";
  private static final String NCBI_JSON_CORE_ORGANISM = "organism";
  private static final String PARAGRAPH = "Paragraph";
  private static final String COMMENT = "Comment";
  private static final String TITLE = "Title";
  private static final String NCBI_JSON_CORE_TITLE = "title";
  private static final String DESCRIPTION = "Description";
  private static final String NCBI_JSON_CORE_DESCRIPTION = "description";
  private static final String INSDC_CENTER_NAME = "INSDC center name";
  private static final String NAME = "Name";
  private static final String OWNER = "Owner";
  private static final String ACCESSION = "accession";
  private static final String GEO = "GEO";
  private static final String ID = "Id";
  private static final String IDS = "Ids";
  private static final String DB_LABEL = "db_label";
  private static final String SAMPLE_NAME = "Sample name";
  private static final String SRA = "SRA";
  private static final String DB = "db";
  private static final String EXTERNAL_ID_JSON = "External Id";
  private static final String INSDC_SECONDARY_ACCESSION = "INSDC secondary accession";
  private static final String SRA_ACCESSION = "SRA accession";
  private static final String NAMESPACE_TAG = "Namespace:";
  private static final String SAMPLE_ATTRIBUTE = "attribute";
  private static final Pattern ANTIBIOGRAM_PATTERN = Pattern.compile("^Antibiogram.*");
  private static final String TARGET = "target";
  private static final String PUBMED = "pubmed";
  private final TaxonomyService taxonomyService;
  private final NcbiAmrConversionService amrConversionService;

  public NcbiSampleConversionService(final TaxonomyService taxonomyService) {
    this.taxonomyService = taxonomyService;
    this.amrConversionService = new NcbiAmrConversionService();
  }

  public Sample convertNcbiXmlElementToSample(final Element sampleElem) {
    final String accession = sampleElem.attributeValue(ACCESSION);
    String sraAccession = null;

    log.info("Handling " + accession);

    final SortedSet<Attribute> attrs = new TreeSet<>();
    final SortedSet<Relationship> rels = new TreeSet<>();
    final SortedSet<Publication> publications = new TreeSet<>();
    final Set<ExternalReference> externalReferences = new TreeSet<>();
    final Attribute organismAttribute;

    String alias = null; // this will be the ENA alias of the sample
    String geoAlias = null;
    String centreName = null; // this will be the ENA centre name of the sample.

    // handle the organism
    String organismIri = null;
    String organismValue = null;
    String geoTag = null;

    for (final Element idElem : XmlPathBuilder.of(sampleElem).path(IDS).elements(ID)) {
      final String attributeValueIdElementDb = idElem.attributeValue(DB);

      if (SRA.equals(attributeValueIdElementDb)) {
        // INSDC SRA IDs get special treatment
        // BSD-1747 - PRIMARY_ID will be mapped to characteristics/SRA accession for
        // NCBI/DDBJ samples, in sync with ENA samples
        if (!SAMPLE_NAME.equals(idElem.attributeValue(DB_LABEL))) {
          String idElemTextTrim = idElem.getTextTrim();

          sraAccession = idElemTextTrim;
          attrs.add(Attribute.build(SRA_ACCESSION, idElemTextTrim));
          attrs.add(Attribute.build(INSDC_SECONDARY_ACCESSION, idElemTextTrim));
        }
      } else if (GENBANK.equalsIgnoreCase(attributeValueIdElementDb)) {
        attrs.add(Attribute.build(COMMON_NAME, idElem.getTextTrim()));
      } else if (SAMPLE_NAME.equals(idElem.attributeValue(DB_LABEL))) {
        // original submitter identifier is stored as the alias to be used as the name
        alias = idElem.getTextTrim();
        centreName = attributeValueIdElementDb;
      } else if (GEO.equalsIgnoreCase(attributeValueIdElementDb)) {
        // GEO IDs get special treatment
        geoTag = attributeValueIdElementDb;
        geoAlias = idElem.getTextTrim();
      }
      // BSD-1765 - Remove synonym tagging of external id's
      else if (ifSomeOtherIdExists(attributeValueIdElementDb)) {
        attrs.add(
            Attribute.build(
                EXTERNAL_ID_JSON,
                idElem.getTextTrim(),
                NAMESPACE_TAG + attributeValueIdElementDb,
                Collections.emptyList(),
                null));
      }
    }

    if (alias == null && geoAlias != null) {
      // if theres no alias but there is a geo alias, then use the geo alias as the
      // alias
      alias = geoAlias;
      geoAlias = null;
    }

    if (geoAlias != null) {
      // if we still have a geo alias, store it as a synonym
      attrs.add(Attribute.build(geoTag, geoAlias));
    }

    if (alias == null) {
      log.warn("Unable to determine sample alias for " + accession + ", falling back to accession");
      alias = accession;
    }

    // override any existing centre name with this, if present
    if (XmlPathBuilder.of(sampleElem).path(OWNER, NAME).exists()) {
      if (!XmlPathBuilder.of(sampleElem).path(OWNER, NAME).text().trim().isEmpty()) {
        centreName = XmlPathBuilder.of(sampleElem).path(OWNER, NAME).text().trim();
      }
    }

    if (centreName == null) {
      // throw new RuntimeException("Unable to determine centre name for "+accession);
      log.warn("Unable to determine centre name for " + accession);
    } else {
      // Note US spelling because NCBI
      attrs.add(Attribute.build(INSDC_CENTER_NAME, centreName));
    }

    // BSD-1748 - Correction in title mapping. Instead of description title, now
    // mapping to title for sync with ENA
    if (XmlPathBuilder.of(sampleElem).path(DESCRIPTION, TITLE).exists()) {
      final String value = XmlPathBuilder.of(sampleElem).path(DESCRIPTION, TITLE).text();
      attrs.add(Attribute.build(NCBI_JSON_CORE_TITLE, value));
    }

    if (XmlPathBuilder.of(sampleElem).path(DESCRIPTION, COMMENT, PARAGRAPH).exists()) {
      final String value =
          XmlPathBuilder.of(sampleElem).path(DESCRIPTION, COMMENT, PARAGRAPH).text().trim();
      /*
       * if (value.length() > 255) {
       * log.warn("Truncating attribute "+key+" for length on "+accession); value =
       * value.substring(0, 252)+"..."; }
       */
      attrs.add(Attribute.build(NCBI_JSON_CORE_DESCRIPTION, value));
    }

    if (XmlPathBuilder.of(sampleElem).path(DESCRIPTION, ORGANISM).exists()) {
      if (XmlPathBuilder.of(sampleElem).path(DESCRIPTION, ORGANISM).attributeExists(TAXONOMY_ID)) {
        final int taxonId =
            getTaxId(
                XmlPathBuilder.of(sampleElem).path(DESCRIPTION, ORGANISM).attribute(TAXONOMY_ID));
        organismIri = taxonomyService.getUriForTaxonId(taxonId);
      }

      if (XmlPathBuilder.of(sampleElem)
          .path(DESCRIPTION, ORGANISM)
          .attributeExists(TAXONOMY_NAME)) {
        organismValue =
            XmlPathBuilder.of(sampleElem).path(DESCRIPTION, ORGANISM).attribute(TAXONOMY_NAME);
      }

      if (organismValue != null) {
        organismAttribute =
            Attribute.build(NCBI_JSON_CORE_ORGANISM, organismValue, organismIri, null);
        attrs.add(organismAttribute);
      }
    }

    // handle attributes
    for (final Element attrElem :
        XmlPathBuilder.of(sampleElem).path(ATTRIBUTES).elements(ATTRIBUTE)) {
      String key = attrElem.attributeValue(ATTRIBUTE_NAME);

      if (key == null || key.isEmpty()) {
        key = attrElem.attributeValue(DISPLAY_NAME);
      }

      final String value = attrElem.getTextTrim();
      /*
       * if (value.length() > 255) {
       * log.warn("Truncating attribute "+key+" for length on "+accession); value =
       * value.substring(0, 252)+"..."; }
       */
      // Dont ignore organism in attributes - ENA Presentation (27.01.2020)

      if (key.equalsIgnoreCase(DESCRIPTION)) {
        attrs.add(Attribute.build(key, value, SAMPLE_ATTRIBUTE, Collections.emptyList(), null));
        continue;
      }

      // if its a gap accession add an external reference too
      if (value.matches("phs[0-9]+")) {
        externalReferences.add(
            ExternalReference.build(
                "https://www.ncbi.nlm.nih.gov/projects/gap/cgi-bin/study.cgi?study_id=" + value));
      }

      // value is a sample accession, assume its a relationship
      if (value.matches("SAM[END]A?[0-9]+")) {
        // if its a self-relationship, then don't add it
        // otherwise add it
        if (!value.equals(accession)) {
          rels.add(Relationship.build(accession, key, value));
        }
      } else {
        // its an attribute

        attrs.add(Attribute.build(key, value, SAMPLE_ATTRIBUTE, Collections.emptyList(), null));
      }
    }

    // handle links
    final XmlPathBuilder links = XmlPathBuilder.of(sampleElem).path(LINKS);

    if (links != null && links.exists()) {
      for (final Element attrElem : XmlPathBuilder.of(sampleElem).path(LINKS).elements(LINK)) {
        final String key = attrElem.attributeValue(TARGET);
        final String value = attrElem.getTextTrim();

        if (key != null && key.equalsIgnoreCase(PUBMED)) {
          publications.add(new Publication.Builder().pubmed_id(value).build());
        }
      }
    }

    // handle model and packages
    // disabled for the moment, do they really add anything? faulcon@2017/01/25
    // yes, ENA want them. But we can name them better. faulcon@2018/02/14
    // TODO safely access these - shouldn't ever be missing but....
    for (final Element modelElem : XmlPathBuilder.of(sampleElem).path(MODELS).elements(MODEL)) {
      attrs.add(Attribute.build(NCBI_SUBMISSION_MODEL, modelElem.getTextTrim()));
    }

    attrs.add(
        Attribute.build(
            NCBI_SUBMISSION_PACKAGE, XmlPathBuilder.of(sampleElem).path(PACKAGE).text()));

    // handle dates
    Instant submissionDate = null;
    Instant publicationDate = null;
    Instant lastUpdate = null;

    final String submission = sampleElem.attributeValue(SUBMISSION_DATE);
    final String publication = sampleElem.attributeValue(PUBLICATION_DATE);
    final String update = sampleElem.attributeValue(LAST_UPDATE);

    if (submission != null && !submission.isEmpty()) {
      submissionDate = Instant.parse(submission + Z);
    }

    if (publication != null && !publication.isEmpty()) {
      publicationDate = Instant.parse(publication + Z);
      // add some INSDC things for standardisation with ENA import
      attrs.add(
          Attribute.build(
              INSDC_FIRST_PUBLIC, DateTimeFormatter.ISO_INSTANT.format(publicationDate)));
    }

    if (update != null && !update.isEmpty()) {
      lastUpdate = Instant.parse(update + Z);
      // add some INSDC things for standardisation with ENA import
      attrs.add(
          Attribute.build(INSDC_LAST_UPDATE, DateTimeFormatter.ISO_INSTANT.format(lastUpdate)));
    }

    if (XmlPathBuilder.of(sampleElem).path(STATUS).attributeExists(STATUS_LOWER_CASE)) {
      final String status =
          XmlPathBuilder.of(sampleElem).path(STATUS).attribute(STATUS_LOWER_CASE).trim();
      final List<String> nonHiddenStatuses = Arrays.asList(LIVE, SUPPRESSED);

      attrs.add(Attribute.build(INSDC_STATUS, status));

      if (!nonHiddenStatuses.contains(status.toLowerCase())) {
        // not a live or suppressed sample, hide
        if (publicationDate != null) {
          publicationDate = publicationDate.atZone(ZoneOffset.UTC).plusYears(1000).toInstant();
        }
      }
    }

    return new Sample.Builder(alias, accession, sraAccession)
        .withRelease(publicationDate)
        .withUpdate(lastUpdate)
        .withCreate(submissionDate)
        .withSubmitted(submissionDate)
        .withAttributes(attrs)
        .withRelationships(rels)
        .withPublications(publications)
        .withExternalReferences(externalReferences)
        .build();
  }

  public Set<StructuredDataTable> convertNcbiXmlElementToStructuredData(
      final Element sampleElem, final Set<StructuredDataTable> amrData) {
    final String accession = sampleElem.attributeValue(ACCESSION);
    final Set<StructuredDataTable> structuredDataTableSet = new HashSet<>();

    // handle AMR data
    if (XmlPathBuilder.of(sampleElem).path(DESCRIPTION, COMMENT).exists()) {
      for (final Element element :
          XmlPathBuilder.of(sampleElem).path(DESCRIPTION, COMMENT).elements("Table")) {
        final String antibiogramClass = element.attributeValue("class");
        if (antibiogramClass != null && ANTIBIOGRAM_PATTERN.matcher(antibiogramClass).matches()) {
          try {
            final Set<Map<String, StructuredDataEntry>> structuredTable =
                amrConversionService.convertStructuredTable(element, null);
            final StructuredDataTable structuredDataTable =
                StructuredDataTable.build(
                    "self.BiosampleImportNCBI", null, "AMR", null, structuredTable);
            structuredDataTableSet.add(structuredDataTable);
          } catch (final AmrParsingException ex) {
            log.error("An error occurred while parsing AMR table", ex);
          }
        }
      }
    }

    if (!structuredDataTableSet.isEmpty()) {
      log.info("Structured data already added from NCBI source " + accession);
    }

    return structuredDataTableSet;
  }

  private boolean ifSomeOtherIdExists(final String idElementValue) {
    return idElementValue != null && !idElementValue.isEmpty();
  }

  private int getTaxId(final String value) {
    if (value == null) {
      throw new RuntimeException("Unable to extract tax id from a null value");
    }

    return Integer.parseInt(value.trim());
  }
}
