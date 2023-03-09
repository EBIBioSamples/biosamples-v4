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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.utils.mongo.SampleReadService;

@Service
public class MessagingService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final SampleReadService sampleReadService;
  private final AmqpTemplate amqpTemplate;

  public MessagingService(
      final SampleReadService sampleReadService,
      final AmqpTemplate amqpTemplate,
      final SampleToMongoSampleConverter sampleToMongoSampleConverter) {
    this.sampleReadService = sampleReadService;
    this.amqpTemplate = amqpTemplate;
  }

  public void sendFileUploadedMessage(final String fileId) {
    log.info("Uploaded file queued " + fileId);

    amqpTemplate.convertAndSend(Messaging.fileUploadExchange, "", fileId);
  }

  void fetchThenSendMessage(final String accession) {
    fetchThenSendMessage(accession, Collections.emptyList());
  }

  void fetchThenSendMessage(
      final String accession, final List<String> existingRelationshipTargets) {
    if (accession == null) {
      throw new IllegalArgumentException("accession cannot be null");
    }
    if (accession.trim().length() == 0) {
      throw new IllegalArgumentException("accession cannot be empty");
    }

    final Optional<Sample> sample = sampleReadService.fetch(accession, Optional.empty());
    if (sample.isPresent()) {
      // for each sample we have a relationship to, update it to index this sample as an
      // inverse
      // relationship
      // TODO do this async
      final List<Sample> related =
          updateInverseRelationships(sample.get(), existingRelationshipTargets);

      // send the original sample with the extras as related samples
      amqpTemplate.convertAndSend(
          Messaging.INDEXING_EXCHANGE,
          Messaging.INDEXING_QUEUE,
          MessageContent.build(sample.get(), null, related, false));
    }
  }

  private List<Sample> updateInverseRelationships(
      final Sample sample, final List<String> existingRelationshipTargets) {
    final List<Future<Optional<Sample>>> futures = new ArrayList<>();

    // remove deleted relationships
    for (final String accession : existingRelationshipTargets) {
      futures.add(sampleReadService.fetchAsync(accession, Optional.empty()));
    }

    for (final Relationship relationship : sample.getRelationships()) {
      if (relationship.getSource() != null
          && relationship.getSource().equals(sample.getAccession())
          && !existingRelationshipTargets.contains(sample.getAccession())) {
        futures.add(sampleReadService.fetchAsync(relationship.getTarget(), Optional.empty()));
      }
    }

    final List<Sample> related = new ArrayList<>();
    for (final Future<Optional<Sample>> sampleFuture : futures) {
      try {
        sampleFuture.get().ifPresent(related::add);
      } catch (final InterruptedException e) {
        log.warn("Interrupted fetching future relationships", e);
      } catch (final ExecutionException e) {
        log.error("Problem fetching future relationships", e);
      }
    }
    return related;
  }

  @Deprecated
  public void sendMessages(final CurationLink curationLink) {
    fetchThenSendMessage(curationLink.getSample());
  }

  @Deprecated
  public void sendMessages(final Sample sample) {
    fetchThenSendMessage(sample.getAccession());
  }

  private List<Sample> getDerivedFromSamples(final Sample sample, final List<Sample> related) {
    for (final Relationship relationship : sample.getRelationships()) {
      if (relationship.getSource().equals(sample.getAccession())) {
        if (relationship.getType().toLowerCase().equals("derived from")) {
          final Optional<Sample> target =
              sampleReadService.fetch(relationship.getTarget(), Optional.empty());
          if (target.isPresent()) {
            if (!related.contains(target.get())) {
              related.add(target.get());
              getDerivedFromSamples(target.get(), related);
            }
          }
        }
      }
    }
    return related;
  }
}
