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

import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import uk.ac.ebi.biosamples.BioSamplesConstants;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSequence;
import uk.ac.ebi.biosamples.mongo.repository.MongoSampleRepository;

// this needs to be the spring exception, not the mongo one
public class MongoAccessionService {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final MongoSampleRepository mongoSampleRepository;
  private final SampleToMongoSampleConverter sampleToMongoSampleConverter;
  private final MongoSampleToSampleConverter mongoSampleToSampleConverter;
  private final MongoOperations mongoOperations;

  public MongoAccessionService(
      final MongoSampleRepository mongoSampleRepository,
      final SampleToMongoSampleConverter sampleToMongoSampleConverter,
      final MongoSampleToSampleConverter mongoSampleToSampleConverter,
      final MongoOperations mongoOperations) {
    this.mongoSampleRepository = mongoSampleRepository;
    this.sampleToMongoSampleConverter = sampleToMongoSampleConverter;
    this.mongoSampleToSampleConverter = mongoSampleToSampleConverter;
    this.mongoOperations = mongoOperations;
  }

  public Sample generateAccession(final Sample sample, final boolean generateSAMEAndSRAAccession) {
    MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);

    mongoSample = accessionAndInsert(mongoSample, generateSAMEAndSRAAccession);

    return mongoSampleToSampleConverter.apply(mongoSample);
  }

  private MongoSample accessionAndInsert(
      MongoSample sample, final boolean generateSAMEAndSRAAccession) {
    log.trace("Generating a new accession");

    final MongoSample originalSample = sample;
    // inspired by Counter collection + Optimistic Loops of
    // https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/

    boolean success = false;
    int numRetry = 0;

    while (!success) {
      // TODO add a timeout here
      try {
        sample = prepare(sample, generateUniqueAccessions(generateSAMEAndSRAAccession));
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }

      try {
        sample = mongoSampleRepository.insertNew(sample);
        success = true;
      } catch (final Exception e) {
        if (++numRetry == BioSamplesConstants.MAX_RETRIES) {
          throw new RuntimeException(
              "Cannot generate a new BioSample accession. please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk");
        }

        sample = originalSample;
      }
    }

    if (generateSAMEAndSRAAccession) {
      log.info("Generated BioSample and SRA accession " + sample);
    } else {
      log.info("Generated BioSample accession " + sample);
    }

    return sample;
  }

  private MongoSample prepare(MongoSample sample, final Accessions accessions) {
    final SortedSet<MongoRelationship> relationships = sample.getRelationships();
    final SortedSet<MongoRelationship> newRelationships = new TreeSet<>();
    for (final MongoRelationship relationship : relationships) {
      // this relationship could not specify a source because the sample is un-accessioned
      // now we are assigning an accession, set the source to the accession
      if (relationship.getSource() == null || relationship.getSource().trim().isEmpty()) {
        newRelationships.add(
            MongoRelationship.build(
                accessions.accession, relationship.getType(), relationship.getTarget()));
      } else {
        newRelationships.add(relationship);
      }
    }

    if (accessions.sraAccession != null) {
      sample
          .getAttributes()
          .add(Attribute.build(BioSamplesConstants.SRA_ACCESSION, accessions.sraAccession));
    }

    sample =
        MongoSample.build(
            sample.getName(),
            accessions.accession,
            accessions.sraAccession != null ? accessions.sraAccession : sample.getSraAccession(),
            sample.getDomain(),
            sample.getWebinSubmissionAccountId(),
            sample.getTaxId(),
            sample.getStatus(),
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

  private Accessions generateUniqueAccessions(final boolean generateSAMEAndSRAAccession) {
    final MongoSequence accessionSeq =
        mongoOperations.findAndModify(
            query(where("_id").is(MongoSample.SEQUENCE_NAME)),
            new Update().inc("seq", 1),
            options().returnNew(true).upsert(true),
            MongoSequence.class);

    if (generateSAMEAndSRAAccession) {
      final MongoSequence sraAccessionSeq =
          mongoOperations.findAndModify(
              query(where("_id").is(MongoSample.SRA_SEQUENCE_NAME)),
              new Update().inc("seq", 1),
              options().returnNew(true).upsert(true),
              MongoSequence.class);

      if (!Objects.isNull(accessionSeq) && !Objects.isNull(sraAccessionSeq)) {
        return new Accessions(
            BioSamplesConstants.ACCESSION_PREFIX + accessionSeq.getSeq(),
            BioSamplesConstants.SRA_ACCESSION_PREFIX + sraAccessionSeq.getSeq());
      } else {
        throw new RuntimeException(
            "Cannot generate a new BioSample and SRA accession. please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk");
      }
    } else {
      if (!Objects.isNull(accessionSeq)) {
        return new Accessions(BioSamplesConstants.ACCESSION_PREFIX + accessionSeq.getSeq(), null);
      } else {
        throw new RuntimeException(
            "Cannot generate a new BioSample accession. please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk");
      }
    }
  }

  public String generateOneSRAAccession() {
    final MongoSequence sraAccessionSeq =
        mongoOperations.findAndModify(
            query(where("_id").is(MongoSample.SRA_SEQUENCE_NAME)),
            new Update().inc("seq", 1),
            options().returnNew(true).upsert(true),
            MongoSequence.class);

    if (!Objects.isNull(sraAccessionSeq)) {
      return BioSamplesConstants.SRA_ACCESSION_PREFIX + sraAccessionSeq.getSeq();
    } else {
      throw new RuntimeException(
          "Cannot generate a new SRA accession number. please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk");
    }
  }

  private static class Accessions {
    private final String accession;
    private final String sraAccession;

    private Accessions(final String accession, final String sraAccession) {
      this.accession = accession;
      this.sraAccession = sraAccession;
    }
  }
}
