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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Publication;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.SortedSet;
import java.util.TreeSet;

@Service
public class EnaSampleToBioSampleConversionService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final EnaSampleXmlEnhancer enaSampleXmlEnhancer;
  private final EraProDao eraProDao;
  private final EnaSampleToBioSampleConverter enaSampleToBioSampleConverter;
  private final PipelinesProperties pipelinesProperties;

  public EnaSampleToBioSampleConversionService(
      final EnaSampleXmlEnhancer enaSampleXmlEnhancer,
      final EraProDao eraProDao,
      final EnaSampleToBioSampleConverter enaSampleToBioSampleConverter,
      final PipelinesProperties pipelinesProperties) {
    this.enaSampleXmlEnhancer = enaSampleXmlEnhancer;
    this.eraProDao = eraProDao;
    this.enaSampleToBioSampleConverter = enaSampleToBioSampleConverter;
    this.pipelinesProperties = pipelinesProperties;
  }

  /** Handles one ENA sample */
  Sample enrichSample(final String accession, final boolean isNcbi)
      throws DocumentException {
    final SampleDBBean sampleDBBean = eraProDao.getSampleDetailsByBioSampleId(accession);

    if (sampleDBBean != null) {
      final String xmlString = sampleDBBean.getSampleXml();
      final SAXReader reader = new SAXReader();
      final Document xml = reader.read(new StringReader(xmlString));
      final Element enaSampleRootElement =
              enaSampleXmlEnhancer.applyAllRules(
              xml.getRootElement(), enaSampleXmlEnhancer.getEnaDatabaseSample(accession));

      // check that we got some content
      if (XmlPathBuilder.of(enaSampleRootElement).path("SAMPLE").exists()) {
        return enrichSample(sampleDBBean, enaSampleRootElement, accession, isNcbi);
      } else {
        log.warn("Unable to find SAMPLE element for " + accession);
      }
    }

    return null;
  }

  /** Enriches one ENA sample */
  private Sample enrichSample(
      final SampleDBBean sampleDBBean,
      final Element enaSampleRootElement,
      final String accession,
      final boolean isNcbi) {
    Sample sample = enaSampleToBioSampleConverter.convert(enaSampleRootElement, accession, isNcbi);

    final SortedSet<Attribute> attributes = new TreeSet<>(sample.getCharacteristics());
    final SortedSet<Publication> publications = new TreeSet<>(sample.getPublications());
    final String lastUpdated = sampleDBBean.getLastUpdate();
    final String firstPublic = sampleDBBean.getFirstPublic();
    final String firstCreated = sampleDBBean.getFirstCreated();
    final String webinId =
            pipelinesProperties.getProxyWebinId(); // sampleDBBean.getSubmissionAccountId();
    final String status = handleStatus(sampleDBBean.getStatus());
    final Long taxId = sampleDBBean.getTaxId();
    final Instant release;
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
              sample.getExternalReferences());
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
              sample.getExternalReferences());
    }

    return Sample.Builder.fromSample(sample)
        .withNoData()
        .withPublications(publications)
        .withSubmittedVia(SubmittedViaType.PIPELINE_IMPORT)
        .build();
  }

  private String handleStatus(final int statusId) {
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

    throw new RuntimeException("Unrecognised STATUS_ID " + statusId);
  }
}
