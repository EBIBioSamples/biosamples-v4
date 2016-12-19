package uk.ac.ebi.biosamples.mongo;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackageClasses=MongoConfig.class)
public class MongoConfig {

}
