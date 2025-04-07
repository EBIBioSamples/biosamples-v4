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
import uk.ac.ebi.biosamples.messaging.MessageContent;
import uk.ac.ebi.biosamples.messaging.Messaging;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;
import uk.ac.ebi.biosamples.solr.service.SampleToSolrSampleConverter;

@Service
public class MessageHandlerSolr {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandlerSolr.class);
  private static final List<String> INDEXABLE_STATUSES =
      Arrays.asList(
          "private",
          "public",
          "live",
          "suppressed",
          "killed",
          "temporary_suppressed",
          "temporary_killed");

  private final SolrSampleRepository repository;
  private final SampleToSolrSampleConverter sampleToSolrSampleConverter;

  public MessageHandlerSolr(
      final SolrSampleRepository repository,
      final SampleToSolrSampleConverter sampleToSolrSampleConverter) {
    this.repository = repository;
    this.sampleToSolrSampleConverter = sampleToSolrSampleConverter;
  }

  @RabbitListener(
      queues = Messaging.INDEXING_QUEUE,
      containerFactory = "biosamplesAgentSolrContainerFactory")
  public void handleIndexing(final MessageContent messageContent) {
    handle(messageContent);
  }

  @RabbitListener(
      queues = Messaging.REINDEXING_QUEUE,
      containerFactory = "biosamplesAgentSolrContainerFactory")
  public void handleReindexing(final MessageContent messageContent) {
    handle(messageContent);
  }

  private void handle(final MessageContent messageContent) {
    if (messageContent.getSample() == null) {
      LOGGER.warn("received message without sample");

      return;
    }

    final Sample sample = messageContent.getSample();

    handleSample(sample, messageContent.getCreationTime());

    for (final Sample related : messageContent.getRelated()) {
      handleSample(related, messageContent.getCreationTime());
    }
  }

  private void handleSample(final Sample sample, final String modifiedTime) {
    final String accession = sample.getAccession();

    if (isIndexingCandidate(sample)) {
      try {
        SolrSample solrSample = sampleToSolrSampleConverter.convert(sample);
        // add the modified time to the solrSample
        final String indexedTime =
            ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        assert solrSample != null;

        solrSample =
            SolrSample.build(
                solrSample.getName(),
                solrSample.getAccession(),
                solrSample.getDomain(),
                solrSample.getWebinSubmissionAcccountId(),
                solrSample.getStatus(),
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

        repository.saveWithoutCommit(solrSample);

        LOGGER.info(String.format("added %s to index", accession));
      } catch (final Exception e) {
        LOGGER.error("failed to index " + accession, e);

        throw e;
      }
    } else {
      if (repository.existsById(accession)) {
        repository.deleteById(accession);
        LOGGER.info(String.format("removed %s from index", accession));
      }
    }
  }

  static boolean isIndexingCandidate(final Sample sample) {
    for (final Attribute attribute : sample.getAttributes()) {
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
