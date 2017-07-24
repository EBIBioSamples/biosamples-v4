package uk.ac.ebi.biosamples;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
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
		return new Queue(Messaging.queueToBeIndexedSolr, true);
	}
	
	@Bean
	public Queue getQueueToBeCurated() {
		return new Queue(Messaging.queueToBeCurated, true);
	}

	// declare exchanges

	@Bean
	public FanoutExchange getExchangeForIndexing() {
		return new FanoutExchange(Messaging.exchangeForIndexing, true, false);
	}

	@Bean
	public FanoutExchange getExchangeForIndexingSolr() {
		return new FanoutExchange(Messaging.exchangeForIndexingSolr, true, false);
	}


	@Bean
	public FanoutExchange getExchangeForCuration() {
		return new FanoutExchange(Messaging.exchangeForCuration, true, false);
	}
	
	
	// bind queues to exchanges

	@Bean
	public Binding bindingForIndexing() {
		return BindingBuilder.bind(getExchangeForIndexingSolr()).to(getExchangeForIndexing());
	}

	@Bean
	public Binding bindingForIndexingSolr() {
		return BindingBuilder.bind(getQueueToBeIndexedSolr()).to(getExchangeForIndexingSolr());
	}

	@Bean
	public Binding bindingForCuration() {
		return BindingBuilder.bind(getQueueToBeCurated()).to(getExchangeForCuration());
	}
	
	//enable messaging in json	
	//note that this class is not the same as the http MessageConverter class	
	@Bean
	public MessageConverter getJackson2MessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

}
