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
package uk.ac.ebi.biosamples.solr;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ols.OlsProcessor;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;
import uk.ac.ebi.biosamples.solr.service.SampleToSolrSampleConverter;

@Service
public class MessageHandlerSolr {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandlerSolr.class);
  private static final List<String> INDEXABLE_STATUSES =
      Arrays.asList(
          "public", "live", "suppressed", "killed", "temporary_suppressed", "temporary_killed");

  private final SolrSampleRepository repository;
  private final SampleToSolrSampleConverter sampleToSolrSampleConverter;
  private final OlsProcessor olsProcessor;

  public MessageHandlerSolr(
      SolrSampleRepository repository,
      SampleToSolrSampleConverter sampleToSolrSampleConverter,
      OlsProcessor olsProcessor) {
    this.repository = repository;
    this.sampleToSolrSampleConverter = sampleToSolrSampleConverter;
    this.olsProcessor = olsProcessor;
  }

  @RabbitListener(
      queues = Messaging.queueToBeIndexedSolr,
      containerFactory = "biosamplesAgentSolrContainerFactory")
  public void handle(MessageContent messageContent) {

    if (messageContent.getSample() == null) {
      LOGGER.warn("received message without sample");
      return;
    }

    Sample sample = messageContent.getSample();
    handleSample(sample, messageContent.getCreationTime());
    for (Sample related : messageContent.getRelated()) {
      handleSample(related, messageContent.getCreationTime());
    }
  }

  private void handleSample(Sample sample, String modifiedTime) {
    final String accession = sample.getAccession();

    if (isIndexingCandidate(sample)) {
      SolrSample solrSample = sampleToSolrSampleConverter.convert(sample);
      // add the modified time to the solrSample
      String indexedTime = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

      assert solrSample != null;

      solrSample =
          SolrSample.build(
              solrSample.getName(),
              solrSample.getAccession(),
              solrSample.getDomain(),
              solrSample.getWebinSubmissionAcccountId(),
              solrSample.getRelease(),
              solrSample.getUpdate(),
              modifiedTime,
              indexedTime,
              solrSample.getAttributeValues(),
              solrSample.getAttributeIris(),
              solrSample.getAttributeUnits(),
              solrSample.getOutgoingRelationships(),
              solrSample.getIncomingRelationships(),
              solrSample.getExternalReferencesData(),
              solrSample.getKeywords());

      // expand ontology terms from OLS // todo move this expansion somewhere else
      //      Set<String> expandedTerms = new HashSet<>();
      //      for (List<String> iris : solrSample.getAttributeIris().values()) {
      //        for (String iri : iris) {
      //          expandedTerms.addAll(
      //              olsProcessor.ancestorsAndSynonyms("efo", iri).stream()
      //                          .map(String::toLowerCase)
      //                          .collect(Collectors.toSet()));
      //          expandedTerms.addAll(olsProcessor.ancestorsAndSynonyms("NCBITaxon", iri).stream()
      //                                                      .map(String::toLowerCase)
      //                                                      .collect(Collectors.toSet()));
      //        }
      //      }
      //      solrSample.getKeywords().addAll(expandedTerms);

      repository.saveWithoutCommit(solrSample);
      LOGGER.info(String.format("added %s to index", accession));
    } else {
      if (repository.existsById(accession)) {
        repository.deleteById(accession);
        LOGGER.info(String.format("removed %s from index", accession));
      }
    }
  }

  static boolean isIndexingCandidate(Sample sample) {
    for (Attribute attribute : sample.getAttributes()) {
      if (attribute.getType().equals("INSDC status")) {
        if (!INDEXABLE_STATUSES.contains(attribute.getValue())) {
          LOGGER.debug(
              String.format(
                  "not indexing %s as INSDC status is %s",
                  sample.getAccession(), attribute.getValue()));
          return false;
        }
      }
    }
    return true;
  }
}
