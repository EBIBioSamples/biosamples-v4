package uk.ac.ebi.biosamples.mongo;

import java.net.UnknownHostException;

import org.springframework.beans.factory.ObjectProvider;
//import org.springframework.boot.autoconfigure.mongo.MongoClientFactory;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;

import uk.ac.ebi.biosamples.mongo.service.CustomWriteConcernResolver;

@Configuration
@EnableMongoRepositories(basePackageClasses=MongoConfig.class)
public class MongoConfig {

	@Bean
	public MongoOperations mongoOperations(MongoDbFactory mongoDbFactory, MongoConverter mongoConverter, CustomWriteConcernResolver customWriteConcernResolver) {
		MongoTemplate ops = new MongoTemplate(mongoDbFactory, mongoConverter);
		ops.setWriteConcernResolver(customWriteConcernResolver);
		return ops;
	}
}
