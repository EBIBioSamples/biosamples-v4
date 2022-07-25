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

import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.SortedSet;
import java.util.TreeSet;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Publication;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
public class EnaSampleTransformationService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final EnaXmlEnhancer enaXmlEnhancer;
  private final EraProDao eraProDao;
  private final EnaElementConverter enaElementConverter;
  private final PipelinesProperties pipelinesProperties;

  public EnaSampleTransformationService(
      final EnaXmlEnhancer enaXmlEnhancer,
      final EraProDao eraProDao,
      final EnaElementConverter enaElementConverter,
      final PipelinesProperties pipelinesProperties) {
    this.enaXmlEnhancer = enaXmlEnhancer;
    this.eraProDao = eraProDao;
    this.enaElementConverter = enaElementConverter;
    this.pipelinesProperties = pipelinesProperties;
  }

  /**
   * Enrich the ENA sample with specific attributes and persist using {@link BioSamplesClient}
   *
   * @return nothing its {@link Void}
   * @throws DocumentException if it fails in XML transformation
   */
  public Sample enrichSample(final String accession, final boolean isNcbi)
      throws DocumentException {
    final SampleDBBean sampleDBBean = eraProDao.getSampleDetailsByBioSampleId(accession);

    if (sampleDBBean != null) {
      return enrichSample(sampleDBBean, accession, isNcbi);
    }

    return null;
  }

  /**
   * Handles one ENA sample
   *
   * @param sampleDBBean {@link SampleDBBean}
   * @throws DocumentException in case of parse errors
   */
  private Sample enrichSample(final SampleDBBean sampleDBBean, String accession, boolean isNcbi)
      throws DocumentException {
    final String xmlString = sampleDBBean.getSampleXml();
    final SAXReader reader = new SAXReader();
    final Document xml = reader.read(new StringReader(xmlString));
    final Element root =
        enaXmlEnhancer.applyAllRules(
            xml.getRootElement(), enaXmlEnhancer.getEnaDatabaseSample(accession));

    // check that we got some content
    if (XmlPathBuilder.of(root).path("SAMPLE").exists()) {
      return enrichSample(sampleDBBean, root, accession, isNcbi);
    } else {
      log.warn("Unable to find SAMPLE element for " + accession);
    }

    return null;
  }

  /** Enriches one ENA sample */
  private Sample enrichSample(
      final SampleDBBean sampleDBBean, final Element root, String accession, boolean isNcbi) {
    Sample sample = enaElementConverter.convert(root);
    final SortedSet<Attribute> attributes = new TreeSet<>(sample.getCharacteristics());
    final SortedSet<Publication> publications = new TreeSet<>(sample.getPublications());
    final SortedSet<ExternalReference> externalReferences =
        new TreeSet<>(sample.getExternalReferences());
    final String lastUpdated = sampleDBBean.getLastUpdate();
    final String firstPublic = sampleDBBean.getFirstPublic();
    final String firstCreated = sampleDBBean.getFirstCreated();
    final String webinId = sampleDBBean.getSubmissionAccountId();
    final String status = handleStatus(sampleDBBean.getStatus());
    final Long taxId = sampleDBBean.getTaxId();
    Instant release;
    Instant update = null;
    Instant create = null;
    Instant submitted = null;

    if (lastUpdated != null) {
      update = Instant.parse(lastUpdated);
      attributes.add(
          Attribute.build("INSDC last update", DateTimeFormatter.ISO_INSTANT.format(update)));
    }

    if (firstPublic != null) {
      release = Instant.parse(firstPublic);
      attributes.add(
          Attribute.build("INSDC first public", DateTimeFormatter.ISO_INSTANT.format(release)));
    } else {
      if (status.equals("private")) {
        release =
            Instant.ofEpochSecond(
                LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
      } else {
        release = Instant.now();
      }
    }

    if (firstCreated != null) {
      create = Instant.parse(firstCreated);
      submitted = Instant.parse(firstCreated);
    }

    attributes.add(Attribute.build("INSDC status", status));

    // add external reference
    externalReferences.add(
        ExternalReference.build("https://www.ebi.ac.uk/ena/browser/view/" + accession));

    // Although update date is passed here, its system generated to time now by
    // webapps-core
    if (!isNcbi) {
      sample =
          Sample.build(
              sample.getName(),
              accession,
              null,
              webinId,
              taxId,
              release,
              update,
              create,
              submitted,
              null,
              attributes,
              sample.getRelationships(),
              externalReferences);
    } else {
      sample =
          Sample.build(
              sample.getName(),
              accession,
              pipelinesProperties.getEnaDomain(),
              webinId,
              taxId,
              release,
              update,
              create,
              submitted,
              null,
              attributes,
              sample.getRelationships(),
              externalReferences);
    }

    return Sample.Builder.fromSample(sample).withNoData().withPublications(publications).build();

    /*  if (amrData != null && !amrData.isEmpty()) {
      bioSamplesWebinClient.persistStructuredData(
              StructuredData.build(sampleAccession, update, amrData));
    }*/
  }

  private String handleStatus(int statusId) {
    if (1 == statusId) {
      return "draft";
    } else if (2 == statusId) {
      return "private";
    } else if (3 == statusId) {
      return "cancelled";
    } else if (4 == statusId) {
      return "public";
    } else if (5 == statusId) {
      return "suppressed";
    } else if (6 == statusId) {
      return "killed";
    } else if (7 == statusId) {
      return "temporary_suppressed";
    } else if (8 == statusId) {
      return "temporary_killed";
    }

    throw new RuntimeException("Unrecognised statusid " + statusId);
  }
}
