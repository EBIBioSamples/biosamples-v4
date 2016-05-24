package uk.ac.ebi.biosamples;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

import uk.ac.ebi.biosamples.models.Messaging;

@SpringBootApplication
public class Application {

	@Bean
	public Queue queue() {
		return new Queue(Messaging.queueToBeLoaded, true);
	}

	@Bean
	public TopicExchange exchange() {
		return new TopicExchange(Messaging.exchangeBioSamples);
	}

	@Bean
	public Binding binding(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(Messaging.queueToBeLoaded);
	}

	@Bean
	public MessageConverter messageConverter() {
		return new MappingJackson2MessageConverter();
	}

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(Application.class, args)));
	}
}
