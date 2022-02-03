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
package uk.ac.ebi.biosamples.mongo.service;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;

// this needs to be the spring exception, not the mongo one

public class MongoAccessionService {

  private Logger log = LoggerFactory.getLogger(getClass());

  private final MongoSampleRepository mongoSampleRepository;
  private final SampleToMongoSampleConverter sampleToMongoSampleConverter;
  private final MongoSampleToSampleConverter mongoSampleToSampleConverter;
  private final String prefix;
  private final BlockingQueue<String> accessionCandidateQueue;
  private int accessionCandidateCounter;

  public MongoAccessionService(
      MongoSampleRepository mongoSampleRepository,
      SampleToMongoSampleConverter sampleToMongoSampleConverter,
      MongoSampleToSampleConverter mongoSampleToSampleConverter,
      String prefix,
      int minimumAccession,
      int queueSize) {
    this.mongoSampleRepository = mongoSampleRepository;
    this.sampleToMongoSampleConverter = sampleToMongoSampleConverter;
    this.mongoSampleToSampleConverter = mongoSampleToSampleConverter;
    this.prefix = prefix;
    this.accessionCandidateCounter = minimumAccession;
    this.accessionCandidateQueue = new LinkedBlockingQueue<>(queueSize);
  }

  public Sample generateAccession(Sample sample) {
    MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);
    mongoSample = accessionAndInsert(mongoSample);
    return mongoSampleToSampleConverter.convert(mongoSample);
  }

  private MongoSample accessionAndInsert(MongoSample sample) {
    log.trace("generating an accession");
    MongoSample originalSample = sample;
    // inspired by Optimistic Loops of
    // https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/
    boolean success = false;
    // TODO limit number of tries
    while (!success) {
      // TODO add a timeout here
      try {
        sample = prepare(sample, accessionCandidateQueue.take());
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      try {
        sample = mongoSampleRepository.insertNew(sample);
        success = true;
      } catch (DuplicateKeyException e) {
        success = false;
        sample = originalSample;
      }
    }
    log.debug("generated accession " + sample);
    return sample;
  }

  private MongoSample prepare(MongoSample sample, String accession) {
    SortedSet<MongoRelationship> relationships = sample.getRelationships();
    SortedSet<MongoRelationship> newRelationships = new TreeSet<>();
    for (MongoRelationship relationship : relationships) {
      // this relationship could not specify a source because the sample is unaccessioned
      // now we are assigning an accession, set the source to the accession
      if (relationship.getSource() == null || relationship.getSource().trim().length() == 0) {
        newRelationships.add(
            MongoRelationship.build(accession, relationship.getType(), relationship.getTarget()));
      } else {
        newRelationships.add(relationship);
      }
    }
    sample =
        MongoSample.build(
            sample.getName(),
            accession,
            sample.getDomain(),
            sample.getWebinSubmissionAccountId(),
            sample.getRelease(),
            sample.getUpdate(),
            sample.getCreate(),
            sample.getSubmitted(),
            sample.getReviewed(),
            sample.getAttributes(),
            sample.getData(),
            newRelationships,
            sample.getExternalReferences(),
            sample.getOrganizations(),
            sample.getContacts(),
            sample.getPublications(),
            sample.getCertificates(),
            sample.getSubmittedVia());
    return sample;
  }

  @PostConstruct
  @Scheduled(fixedDelay = 100)
  public synchronized void prepareAccessions() {
    long startTime = System.nanoTime();

    // check that all accessions are still available
    Iterator<String> itCandidate = accessionCandidateQueue.iterator();
    while (itCandidate.hasNext()) {
      String accessionCandidate = itCandidate.next();
      if (mongoSampleRepository.exists(accessionCandidate)) {
        log.debug("Removing accession " + accessionCandidate + " from queue because now assigned");
        itCandidate.remove();
      }
    }

    Sort sort = new Sort(Sort.Direction.ASC, "accessionNumber");
    try (Stream<MongoSample> stream =
        mongoSampleRepository.findByAccessionPrefixIsAndAccessionNumberGreaterThanEqual(
            prefix, accessionCandidateCounter, sort)) {
      Iterator<MongoSample> streamIt = stream.iterator();
      Integer streamAccessionNumber = null;
      if (streamIt.hasNext()) {
        streamAccessionNumber = streamIt.next().getAccessionNumber();
      }
      while (accessionCandidateQueue.remainingCapacity() > 0) {

        if (streamAccessionNumber == null || streamAccessionNumber > accessionCandidateCounter) {
          String accessionCandidate = prefix + accessionCandidateCounter;
          if (accessionCandidateQueue.offer(accessionCandidate)) {
            // successfully added, move to next
            log.trace("Added to accession pool " + accessionCandidate);
            accessionCandidateCounter += 1;
          } else {
            // failed, queue full
            break;
          }
        } else {
          // update stream to next accession
          try {
            streamAccessionNumber = streamIt.next().getAccessionNumber();
            accessionCandidateCounter += 1;
            log.trace("Updating stream to " + prefix + streamAccessionNumber);
          } catch (NoSuchElementException e) {
            // end of stream
            streamAccessionNumber = null;
            log.trace("Reached end of stream");
          }
          // move back and try again
        }
      }
    }

    long endTime = System.nanoTime();
    log.trace("Populated accession pool in " + ((endTime - startTime) / 1000000) + "ms");
  }
}
