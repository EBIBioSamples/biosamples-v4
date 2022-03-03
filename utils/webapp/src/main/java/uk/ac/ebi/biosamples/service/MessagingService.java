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
import uk.ac.ebi.biosamples.model.StaticViewWrapper;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.utils.mongo.SampleReadService;

@Service
public class MessagingService {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final SampleReadService sampleReadService;
  private final AmqpTemplate amqpTemplate;
  private final MongoSampleRepository mongoSampleRepository;
  private final SampleToMongoSampleConverter sampleToMongoSampleConverter;

  public MessagingService(
      SampleReadService sampleReadService,
      AmqpTemplate amqpTemplate,
      MongoSampleRepository mongoSampleRepository,
      SampleToMongoSampleConverter sampleToMongoSampleConverter) {
    this.sampleReadService = sampleReadService;
    this.amqpTemplate = amqpTemplate;
    this.mongoSampleRepository = mongoSampleRepository;
    this.sampleToMongoSampleConverter = sampleToMongoSampleConverter;
  }

  public void sendFileUploadedMessage(final String fileId) {
    log.info("Uploaded file queued " + fileId);

    amqpTemplate.convertAndSend(Messaging.fileUploadExchange, "", fileId);
  }

  public void fetchThenSendMessage(String accession) {
    fetchThenSendMessage(accession, Collections.emptyList());
  }

  public void fetchThenSendMessage(String accession, List<String> existingRelationshipTargets) {
    if (accession == null) throw new IllegalArgumentException("accession cannot be null");
    if (accession.trim().length() == 0)
      throw new IllegalArgumentException("accession cannot be empty");

    Optional<Sample> sample = sampleReadService.fetch(accession, Optional.empty());
    if (sample.isPresent()) {
      // save sample with curations and relationships in static view collection
      mongoSampleRepository.insertSampleToCollection(
          sampleToMongoSampleConverter.convert(sample.get()),
          StaticViewWrapper.StaticView.SAMPLES_CURATED);

      // for each sample we have a relationship to, update it to index this sample as an
      // inverse
      // relationship
      // TODO do this async
      List<Sample> related = updateInverseRelationships(sample.get(), existingRelationshipTargets);

      // send the original sample with the extras as related samples
      amqpTemplate.convertAndSend(
          Messaging.exchangeForIndexingSolr,
          "",
          MessageContent.build(sample.get(), null, related, false));
    }
  }

  private List<Sample> updateInverseRelationships(
      Sample sample, List<String> existingRelationshipTargets) {
    List<Future<Optional<Sample>>> futures = new ArrayList<>();

    // remove deleted relationships
    for (String accession : existingRelationshipTargets) {
      futures.add(sampleReadService.fetchAsync(accession, Optional.empty()));
    }

    for (Relationship relationship : sample.getRelationships()) {
      if (relationship.getSource() != null
          && relationship.getSource().equals(sample.getAccession())
          && !existingRelationshipTargets.contains(sample.getAccession())) {
        futures.add(sampleReadService.fetchAsync(relationship.getTarget(), Optional.empty()));
      }
    }

    List<Sample> related = new ArrayList<>();
    for (Future<Optional<Sample>> future : futures) {
      try {
        Optional<Sample> optionalSample = future.get();
        if (optionalSample.isPresent()) {
          related.add(optionalSample.get());
          // todo if we  add inverse relationships we also have to think about deleting
          // them
          mongoSampleRepository.insertSampleToCollection(
              sampleToMongoSampleConverter.convert(optionalSample.get()),
              StaticViewWrapper.StaticView.SAMPLES_CURATED);
        }
      } catch (InterruptedException e) {
        log.warn("Interrupted fetching future relationships", e);
      } catch (ExecutionException e) {
        log.error("Problem fetching future relationships", e);
      }
    }
    return related;
  }

  @Deprecated
  public void sendMessages(CurationLink curationLink) {
    fetchThenSendMessage(curationLink.getSample());
  }

  @Deprecated
  public void sendMessages(Sample sample) {
    fetchThenSendMessage(sample.getAccession());
  }

  public List<Sample> getDerivedFromSamples(Sample sample, List<Sample> related) {

    for (Relationship relationship : sample.getRelationships()) {
      if (relationship.getSource().equals(sample.getAccession())) {
        if (relationship.getType().toLowerCase().equals("derived from")) {
          Optional<Sample> target =
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
