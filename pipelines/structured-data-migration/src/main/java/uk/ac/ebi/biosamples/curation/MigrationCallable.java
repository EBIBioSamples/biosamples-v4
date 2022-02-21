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
package uk.ac.ebi.biosamples.curation;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoStructuredDataRepository;

public class MigrationCallable implements Callable<PipelineResult> {
  private static final Logger LOG = LoggerFactory.getLogger(MigrationCallable.class);
  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

  private final MongoSample mongoSample;
  private final MongoSampleRepository mongoSampleRepository;
  private final MongoStructuredDataRepository mongoStructuredDataRepository;

  public MigrationCallable(
      MongoSample mongoSample,
      MongoSampleRepository mongoSampleRepository,
      MongoStructuredDataRepository mongoStructuredDataRepository) {
    this.mongoSample = mongoSample;
    this.mongoSampleRepository = mongoSampleRepository;
    this.mongoStructuredDataRepository = mongoStructuredDataRepository;
  }

  @Override
  public PipelineResult call() {
    int modifiedRecords = 0;
    boolean success = true;

    MongoSample mongoSampleWithoutStructuredData =
        MongoSample.build(
            mongoSample.getName(),
            mongoSample.getAccession(),
            mongoSample.getDomain(),
            mongoSample.getWebinSubmissionAccountId(),
            mongoSample.getRelease(),
            mongoSample.getUpdate(),
            mongoSample.getCreate(),
            mongoSample.getSubmitted(),
            mongoSample.getReviewed(),
            mongoSample.getAttributes(),
            null,
            mongoSample.getRelationships(),
            mongoSample.getExternalReferences(),
            mongoSample.getOrganizations(),
            mongoSample.getContacts(),
            mongoSample.getPublications(),
            mongoSample.getCertificates(),
            mongoSample.getSubmittedVia());

    mongoSampleRepository.save(mongoSampleWithoutStructuredData);
    // mongoSample.getData().removeAll(mongoSample.getData());
    // mongoSampleRepository.save(mongoSample);
    modifiedRecords++;

    return new PipelineResult(mongoSample.getAccession(), modifiedRecords, success);
  }
}
