package uk.ac.ebi.biosamples;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;


@SpringBootApplication
public class Application {

	//to handle dots in attribute types
	@Bean
	public MappingMongoConverter getMappingMongoConverter(MongoDbFactory factory, MongoMappingContext mongoMappingContext) {
		
		DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
		MappingMongoConverter mappingMongoConverter = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
		mappingMongoConverter.setMapKeyDotReplacement("\\+");
		return mappingMongoConverter;
	}
	
	//converter for web JSON messages
    @Bean
    public MessageConverter messageConverter() {
        return new MappingJackson2MessageConverter();
    }
    
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
