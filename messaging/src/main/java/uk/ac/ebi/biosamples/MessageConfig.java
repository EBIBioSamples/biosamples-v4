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
package uk.ac.ebi.biosamples;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
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
  @Bean(name = "solrQueue")
  public Queue getQueueToBeIndexedSolr() {
    return QueueBuilder.durable(Messaging.INDEXING_QUEUE)
        .withArgument("x-dead-letter-exchange", Messaging.exchangeDeadLetter)
        .build();
  }

  @Bean(name = "reindxingQueue")
  public Queue getReindexingQueue() {
    return QueueBuilder.durable(Messaging.REINDEXING_QUEUE)
        .build();
  }

  @Bean(name = "uploaderQueue")
  public Queue getFileUploaderQueue() {
    return QueueBuilder.durable(Messaging.fileUploadQueue).build();
  }

  // this queue sets up a delay before messages are requeued on the original solr indexing queue
  // do not consume from this queue
  // instead, allow all messages to reach the end of their "lifetime" (time-to-live) and then
  // requeue them as if they were being sent to a dead-letter queue
  @Bean(name = "deadLetterQueue")
  public Queue getQueueRetryDeadLetter() {
    return QueueBuilder.durable(Messaging.queueRetryDeadLetter)
        .withArgument("x-message-ttl", 30000) // 60 seconds
        .withArgument("x-dead-letter-exchange", Messaging.INDEXING_EXCHANGE)
        .build();
  }

  // declare exchanges
  @Bean(name = "solrExchange")
  public Exchange getExchangeForIndexingSolr() {
    return ExchangeBuilder.directExchange(Messaging.INDEXING_EXCHANGE).durable(true).build();
  }

  @Bean(name = "reindxingEchange")
  public Exchange getReIndexingExcahnge() {
    return ExchangeBuilder.directExchange(Messaging.REINDEXING_EXCHANGE).durable(true).build();
  }

  @Bean(name = "uploaderExchange")
  public Exchange getFileUploaderExchange() {
    return ExchangeBuilder.fanoutExchange(Messaging.fileUploadExchange).durable(true).build();
  }

  @Bean(name = "deadLetterExchange")
  public Exchange getExchangeDeadLetter() {
    return ExchangeBuilder.directExchange(Messaging.exchangeDeadLetter).durable(true).build();
  }

  // bind queues to exchanges
  @Bean(name = "solrBindings")
  public Binding bindingForIndexingSolr() {
    return BindingBuilder.bind(getQueueToBeIndexedSolr())
        .to(getExchangeForIndexingSolr())
        .with(Messaging.INDEXING_QUEUE)
        .noargs();
  }

  @Bean(name = "reindexingBinding")
  public Binding getReIndexingBinding() {
    return BindingBuilder.bind(getReindexingQueue())
        .to(getReIndexingExcahnge())
        .with(Messaging.REINDEXING_QUEUE)
        .noargs();
  }

  @Bean(name = "uploaderBindings")
  public Binding bindingForFileUploaderQueues() {
    return BindingBuilder.bind(getFileUploaderQueue())
        .to(getFileUploaderExchange())
        .with(Messaging.fileUploadQueue)
        .noargs();
  }

  // enable messaging in json
  // note that this class is not the same as the http MessageConverter class
  @Bean
  public MessageConverter getJackson2MessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
