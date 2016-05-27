package uk.ac.ebi.biosamples;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

@Configuration
public class MessageConfig {

	//declare queues
	
	@Bean
	public Queue getQueueToBeLoaded() {
		return new Queue(Messaging.queueToBeLoaded, true);
	}	
	@Bean
	public Queue getQueueToBeIndexedSolr() {
		return new Queue(Messaging.queueToBeIndexedSolr, true);
	}	
	@Bean
	public Queue getQueueToBeIndexedNeo4J() {
		return new Queue(Messaging.queueToBeIndexedNeo4J, true);
	}
	
	//declare exchanges
	
    @Bean
    public FanoutExchange getExchangeForLoading() {
        return new FanoutExchange(Messaging.exchangeForLoading, true, false);
    }	
    @Bean
    public FanoutExchange getExchangeForIndexing() {
        return new FanoutExchange(Messaging.exchangeForIndexing, true, false);
    }
    
    //bind queues to exchanges

    //bind for loading exchange
    
	@Bean
	public Binding bindingLoading(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(getQueueToBeLoaded()).to(getExchangeForLoading());
	}
	@Bean
	public Binding bindingLoadingSolr(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(getQueueToBeIndexedSolr()).to(getExchangeForLoading());
	}
	@Bean
	public Binding bindingLoadingNeo4J(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(getQueueToBeIndexedNeo4J()).to(getExchangeForLoading());
	}

	//bind for indexing exchange
	
	@Bean
	public Binding bindingIndexingSolr(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(getQueueToBeIndexedSolr()).to(getExchangeForIndexing());
	}
	@Bean
	public Binding bindingIndexingNeo4J(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(getQueueToBeIndexedNeo4J()).to(getExchangeForIndexing());
	}
	
	//add messaging in json
	
	@Bean
	public MessageConverter messageConverter() {
		return new MappingJackson2MessageConverter();
	}

}
