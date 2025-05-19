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

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

@Service
public class MongoInverseRelationshipService {

  private final MongoTemplate mongoTemplate;

  public MongoInverseRelationshipService(final MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public MongoSample addInverseRelationships(final MongoSample mongoSample) {
    final String accession = mongoSample.getAccession();
    if (accession == null) {
      return mongoSample;
    }

    final Query query =
        new BasicQuery("{'relationships.target':'" + accession + "'}", "{'relationships.$':1}");
    for (final MongoSample other : mongoTemplate.find(query, MongoSample.class)) {
      for (final MongoRelationship relationship : other.getRelationships()) {
        if (relationship.getTarget().equals(accession)) {
          mongoSample.getRelationships().add(relationship);
        }
      }
    }

    return mongoSample;
  }

  public List<String> getInverseRelationshipsTargets(final String accession) {
    final List<String> relTargetAccessionList = new ArrayList<>();
    final Query query =
        new BasicQuery("{'relationships.target':'" + accession + "'}", "{'relationships.$':1}");
    for (final MongoSample other : mongoTemplate.find(query, MongoSample.class)) {
      for (final MongoRelationship relationship : other.getRelationships()) {
        if (relationship.getTarget().equals(accession)) {
          relTargetAccessionList.add(relationship.getSource());
        }
      }
    }

    return relTargetAccessionList;
  }
}
