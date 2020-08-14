/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.core.MongoOperations;
import uk.ac.ebi.biosamples.mongo.model.MongoSampleTab;

public class MongoSampleTabRepositoryImpl implements MongoSampleTabRepositoryCustom {

  private final MongoOperations mongoOperations;

  public MongoSampleTabRepositoryImpl(MongoOperations mongoOperations) {
    this.mongoOperations = mongoOperations;
  }

  /**
   * Uses the MongoOperations.insert() method to only insert new documents and will throw errors in
   * all other cases.
   */
  @Override
  public MongoSampleTab insertNew(MongoSampleTab sampletab) {
    mongoOperations.insert(sampletab);
    return sampletab;
  }
}
