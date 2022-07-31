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

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSequence;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;

// this needs to be the spring exception, not the mongo one
public class MongoAccessionService {
  private static final int MAX_RETRIES = 5;
  private Logger log = LoggerFactory.getLogger(getClass());

  private final MongoSampleRepository mongoSampleRepository;
  private final SampleToMongoSampleConverter sampleToMongoSampleConverter;
  private final MongoSampleToSampleConverter mongoSampleToSampleConverter;
  private final String prefix;
  private final MongoOperations mongoOperations;

  public MongoAccessionService(
      final MongoSampleRepository mongoSampleRepository,
      final SampleToMongoSampleConverter sampleToMongoSampleConverter,
      final MongoSampleToSampleConverter mongoSampleToSampleConverter,
      final String prefix,
      final MongoOperations mongoOperations) {
    this.mongoSampleRepository = mongoSampleRepository;
    this.sampleToMongoSampleConverter = sampleToMongoSampleConverter;
    this.mongoSampleToSampleConverter = mongoSampleToSampleConverter;
    this.prefix = prefix;
    this.mongoOperations = mongoOperations;
  }

  public Sample generateAccession(final Sample sample) {
    MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);

    mongoSample = accessionAndInsert(mongoSample);
    return mongoSampleToSampleConverter.apply(mongoSample);
  }

  private MongoSample accessionAndInsert(MongoSample sample) {
    log.trace("generating an accession");

    final MongoSample originalSample = sample;
    // inspired by Counter collection + Optimistic Loops of
    // https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/

    boolean success = false;
    int numRetry = 0;

    while (!success) {
      // TODO add a timeout here
      try {
        sample = prepare(sample, getAccession());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      try {
        sample = mongoSampleRepository.insertNew(sample);
        success = true;
      } catch (Exception e) {
        if (++numRetry == MAX_RETRIES) {
          throw new RuntimeException(
              "Cannot generate a new BioSample accession. please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk");
        }

        success = false;
        sample = originalSample;
      }
    }

    log.info("generated accession " + sample);

    return sample;
  }

  private String getAccession() {
    return prefix + generateUniqueAccession(MongoSample.SEQUENCE_NAME);
  }

  private MongoSample prepare(MongoSample sample, String accession) {
    SortedSet<MongoRelationship> relationships = sample.getRelationships();
    SortedSet<MongoRelationship> newRelationships = new TreeSet<>();
    for (final MongoRelationship relationship : relationships) {
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
            sample.getTaxId(),
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

  public long generateUniqueAccession(final String seqName) {
    final MongoSequence counter =
        mongoOperations.findAndModify(
            query(where("_id").is(seqName)),
            new Update().inc("seq", 1),
            options().returnNew(true).upsert(true),
            MongoSequence.class);

    if (!Objects.isNull(counter)) {
      return counter.getSeq();
    } else {
      throw new RuntimeException(
          "Cannot generate a new BioSample accession. please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk");
    }
  }
}
