package uk.ac.ebi.biosamples;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class MessageConfig {

	// declare queues

	@Bean
	public Queue getQueueToBeIndexedSolr() {		
		return QueueBuilder.durable(Messaging.queueToBeIndexedSolr)
				.withArgument("x-dead-letter-exchange", Messaging.exchangeDeadLetter)
				.build();
	}

	
	//this queue sets up a delay before messages are requeued on the original solr indexing queue
	//do not consume from this queue
	//instead, allow all messages to reach the end of their "lifetime" (time-to-live) and then 
	//requeue them as if they were being sent to a dead-letter queue
	@Bean
	public Queue getQueueRetryDeadLetter() {		
		return QueueBuilder.durable(Messaging.queueRetryDeadLetter)
				.withArgument("x-message-ttl", 30000) //30 seconds
				.withArgument("x-dead-letter-exchange", Messaging.exchangeForIndexingSolr)
				.build();
	}

	// declare exchanges

	@Bean
	public Exchange getExchangeForIndexingSolr() {
		return ExchangeBuilder.fanoutExchange(Messaging.exchangeForIndexingSolr).durable(true).build();
	}

	@Bean
	public Exchange getExchangeDeadLetter() {		
		return ExchangeBuilder.directExchange(Messaging.exchangeDeadLetter).durable(true).build();
	}

	
	
	// bind queues to exchanges

	@Bean
	public Binding bindingForIndexingSolr() {
		return BindingBuilder.bind(getQueueToBeIndexedSolr()).to(getExchangeForIndexingSolr()).with(Messaging.queueToBeIndexedSolr).noargs();
	}
	
	//enable messaging in json	
	//note that this class is not the same as the http MessageConverter class	
	@Bean
	public MessageConverter getJackson2MessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

}
