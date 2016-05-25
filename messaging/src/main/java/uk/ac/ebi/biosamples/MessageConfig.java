package uk.ac.ebi.biosamples;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

@Configuration
public class MessageConfig {

	@Bean
	public Queue queue() {
		return new Queue(Messaging.queueToBeLoaded, true);
	}
	
	@Bean
	public MessageConverter messageConverter() {
		return new MappingJackson2MessageConverter();
	}

}
