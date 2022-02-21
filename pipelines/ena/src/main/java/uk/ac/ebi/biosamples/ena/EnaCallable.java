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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ega.EgaSampleExporter;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class EnaCallable implements Callable<Void> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String DDBJ_SAMPLE_PREFIX = "SAMD";
  private static final String NCBI_SAMPLE_PREFIX = "SAMN";
  private static final String SUPPRESSED = "suppressed";
  private static final String TEMPORARY_SUPPRESSED = "temporary_suppressed";
  private static final String KILLED = "killed";
  private static final String TEMPORARY_KILLED = "temporary_killed";
  private final String sampleAccession;
  private final String egaId;
  private final BioSamplesClient bioSamplesWebinClient;
  private final EnaXmlEnhancer enaXmlEnhancer;
  private final EraProDao eraProDao;
  private final EnaElementConverter enaElementConverter;
  private final EgaSampleExporter egaSampleExporter;
  private final Set<StructuredDataTable> amrData;
  private boolean suppressionHandler;
  private boolean killedHandler;
  private int statusId;

  public EnaCallable(
      String sampleAccession,
      String egaId,
      int statusId,
      BioSamplesClient bioSamplesWebinClient,
      EnaXmlEnhancer enaXmlEnhancer,
      EnaElementConverter enaElementConverter,
      EgaSampleExporter egaSampleExporter,
      EraProDao eraProDao,
      boolean suppressionHandler,
      boolean killedHandler,
      Set<StructuredDataTable> amrData) {
    this.sampleAccession = sampleAccession;
    this.egaId = egaId;
    this.statusId = statusId;
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.enaXmlEnhancer = enaXmlEnhancer;
    this.enaElementConverter = enaElementConverter;
    this.egaSampleExporter = egaSampleExporter;
    this.eraProDao = eraProDao;
    this.suppressionHandler = suppressionHandler;
    this.killedHandler = killedHandler;
    this.amrData = amrData;
  }

  @Override
  public Void call() throws Exception {
    if (egaId != null && !egaId.isEmpty()) {
      return egaSampleExporter.populateAndSubmitEgaData(egaId);
    } else if (suppressionHandler) {
      return checkAndUpdateSuppressedSample();
    } else if (killedHandler) {
      return checkAndUpdateKilledSamples();
    } else {
      return enrichAndPersistEnaSample();
    }
  }

  /**
   * Enrich the ENA sample with specific attributes and persist using {@link BioSamplesClient}
   *
   * @return nothing its {@link Void}
   * @throws DocumentException if it fails in XML transformation
   */
  private Void enrichAndPersistEnaSample() throws DocumentException {
    log.info("HANDLING " + sampleAccession);

    final SampleDBBean sampleDBBean = eraProDao.getAllSampleData(this.sampleAccession);

    if (sampleDBBean != null) {
      handleEnaSample(sampleDBBean);
    }

    return null;
  }

  /**
   * Handles one ENA sample
   *
   * @param sampleDBBean {@link SampleDBBean}
   * @throws DocumentException in case of parse errors
   */
  private void handleEnaSample(final SampleDBBean sampleDBBean) throws DocumentException {
    final String xmlString = sampleDBBean.getSampleXml();
    final SAXReader reader = new SAXReader();
    final Document xml = reader.read(new StringReader(xmlString));
    final Element root =
        enaXmlEnhancer.applyAllRules(
            xml.getRootElement(), enaXmlEnhancer.getEnaDatabaseSample(sampleAccession));

    // check that we got some content
    if (XmlPathBuilder.of(root).path("SAMPLE").exists()) {
      enrichEnaSample(sampleDBBean, root);
    } else {
      log.warn("Unable to find SAMPLE element for " + sampleAccession);
    }

    log.trace("HANDLED " + sampleAccession);
  }

  /**
   * Enriches one ENA sample
   *
   * @param sampleDBBean {@link SampleDBBean}
   * @param root The XML {@link Element}
   */
  private void enrichEnaSample(final SampleDBBean sampleDBBean, final Element root) {
    Sample sample = enaElementConverter.convert(root);
    final SortedSet<Attribute> attributes = new TreeSet<>(sample.getCharacteristics());
    final SortedSet<ExternalReference> externalReferences =
        new TreeSet<>(sample.getExternalReferences());
    final String lastUpdated = sampleDBBean.getLastUpdate();
    final String firstPublic = sampleDBBean.getFirstPublic();
    final String firstCreated = sampleDBBean.getFirstCreated();
    final String webinId = sampleDBBean.getSubmissionAccountId();
    final String status = handleStatus(sampleDBBean.getStatus());
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
      release = Instant.now();
    }

    if (firstCreated != null) {
      create = Instant.parse(firstCreated);
      submitted = Instant.parse(firstCreated);
    }

    attributes.add(Attribute.build("INSDC status", status));

    // add external reference
    externalReferences.add(
        ExternalReference.build("https://www.ebi.ac.uk/ena/browser/view/" + this.sampleAccession));

    // Although update date is passed here, its system generated to time now by
    // webapps-core
    sample =
        Sample.build(
            sample.getName(),
            this.sampleAccession,
            null,
            webinId,
            release,
            update,
            create,
            submitted,
            null,
            attributes,
            sample.getRelationships(),
            externalReferences);

    sample = Sample.Builder.fromSample(sample).withNoData().build();
    bioSamplesWebinClient.persistSampleResource(sample);

    if (amrData != null && !amrData.isEmpty()) {
      bioSamplesWebinClient.persistStructuredData(
          StructuredData.build(sampleAccession, update, amrData));
    }
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

  /**
   * Checks samples from ENA which is SUPPRESSED and takes necessary action, i.e. update status if
   * status is different in BioSamples, else persist
   *
   * @return {@link Void}
   */
  private Void checkAndUpdateSuppressedSample() throws DocumentException {
    final List<String> curationDomainBlankList = new ArrayList<>();
    curationDomainBlankList.add("");

    try {
      Optional<Resource<Sample>> optionalSampleResource =
          bioSamplesWebinClient.fetchSampleResource(
              this.sampleAccession, Optional.of(curationDomainBlankList));

      if (optionalSampleResource.isPresent()) {
        final Sample sample = optionalSampleResource.get().getContent();
        boolean persistRequired = true;

        for (Attribute attribute : sample.getAttributes()) {
          if (attribute.getType().equals("INSDC status") && attribute.getValue().equals(SUPPRESSED)
              || attribute.getValue().equalsIgnoreCase(TEMPORARY_SUPPRESSED)) {
            persistRequired = false;
            break;
          }
        }

        if (persistRequired) {
          sample.getAttributes().removeIf(attr -> attr.getType().contains("INSDC status"));
          sample
              .getAttributes()
              .add(
                  Attribute.build(
                      "INSDC status", statusId == 5 ? SUPPRESSED : TEMPORARY_SUPPRESSED));
          log.info("Updating status to suppressed of sample: " + this.sampleAccession);
          bioSamplesWebinClient.persistSampleResource(sample);
        }
      } else {
        if (ifNcbiDdbj()) {
          log.info("Accession doesn't exist " + this.sampleAccession + " creating the same");
          return enrichAndPersistEnaSample();
        }
      }
    } catch (final RuntimeException e) {
      if (e.getMessage().contains("404")) {
        log.info("Accession doesn't exist " + this.sampleAccession + " creating the same");
        enrichAndPersistEnaSample();
      } else {
        log.error("Failed to update status of ENA sample " + sampleAccession + " to SUPPRESSED");
      }
    }

    return null;
  }

  /**
   * Checks samples from ENA which is KILLED and takes necessary action, i.e. update status if
   * status is different in BioSamples, else persist
   *
   * @return {@link Void}
   */
  private Void checkAndUpdateKilledSamples() throws DocumentException {
    final List<String> curationDomainBlankList = new ArrayList<>();
    curationDomainBlankList.add("");

    try {
      Optional<Resource<Sample>> optionalSampleResource =
          bioSamplesWebinClient.fetchSampleResource(
              this.sampleAccession, Optional.of(curationDomainBlankList));

      if (optionalSampleResource.isPresent()) {
        final Sample sample = optionalSampleResource.get().getContent();
        boolean persistRequired = true;

        for (Attribute attribute : sample.getAttributes()) {
          if (attribute.getType().equals("INSDC status")
              && (attribute.getValue().equals(KILLED)
                  || attribute.getValue().equals(TEMPORARY_KILLED))) {
            persistRequired = false;
            break;
          }
        }

        if (persistRequired) {
          sample.getAttributes().removeIf(attr -> attr.getType().contains("INSDC status"));
          sample
              .getAttributes()
              .add(Attribute.build("INSDC status", statusId == 6 ? KILLED : TEMPORARY_KILLED));
          log.info("Updating status to killed of sample: " + this.sampleAccession);
          bioSamplesWebinClient.persistSampleResource(sample);
        }
      } else {
        if (ifNcbiDdbj()) {
          log.info("Accession doesn't exist " + this.sampleAccession + " creating the same");
          return enrichAndPersistEnaSample();
        }
      }
    } catch (final Exception e) {
      if (e.getMessage().contains("404")) {
        log.info("Accession doesn't exist " + this.sampleAccession + " creating the same");
        enrichAndPersistEnaSample();
      } else {
        log.error("Failed to update status of ENA sample " + sampleAccession + " to KILLED");
      }
    }

    return null;
  }

  /**
   * True if NCBI/DDBJ sample
   *
   * @return true if NCBI/DDBJ sample
   */
  private boolean ifNcbiDdbj() {
    return !this.sampleAccession.startsWith(NCBI_SAMPLE_PREFIX)
        && !this.sampleAccession.startsWith(DDBJ_SAMPLE_PREFIX);
  }
}
