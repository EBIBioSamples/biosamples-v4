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
package uk.ac.ebi.biosamples.mongo.repo;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

public interface MongoSampleRepository
    extends MongoRepository<MongoSample, String>, MongoSampleRepositoryCustom {

  Page<MongoSample> findByExternalReferences_Hash(String urlHash, Pageable pageable);

  @Query("{ $and : [{ 'domain' : ?0 },{'name' : ?1 }]}")
  List<MongoSample> findByDomainAndName(String domain, String name);

  @Query("{ $and : [{ 'webinSubmissionAccountId' : ?0 },{'name' : ?1 }]}")
  List<MongoSample> findByWebinSubmissionAccountIdAndName(
      String webinSubmissionAccountId, String name);

  @Query("{ $and : [{ accessionPrefix : ?0 },{accessionNumber : { $gte : ?1 }}]}")
  Stream<MongoSample> findByAccessionPrefixIsAndAccessionNumberGreaterThanEqual(
      String accessionPrefix, int accessionNumber, Sort sort);
}
