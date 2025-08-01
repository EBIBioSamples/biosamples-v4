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
package uk.ac.ebi.biosamples.mongo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import uk.ac.ebi.biosamples.mongo.service.CustomWriteConcernResolver;

@Configuration
@EnableMongoRepositories(basePackages = "uk.ac.ebi.biosamples.mongo.repository")
public class MongoConfig {
  @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/biosamples}")
  private String mongoDbUrl;

  @Bean
  public MongoTemplate mongoTemplate(
      final MongoDatabaseFactory mongoDatabaseFactory,
      final MongoConverter mongoConverter,
      final CustomWriteConcernResolver customWriteConcernResolver) {
    final MongoTemplate ops = new MongoTemplate(mongoDatabaseFactory, mongoConverter);
    ops.setWriteConcernResolver(customWriteConcernResolver);

    return ops;
  }

  @Bean
  public MongoDatabaseFactory mongoDatabaseFactory() {
    return new SimpleMongoClientDatabaseFactory(mongoDbUrl);
  }
}
