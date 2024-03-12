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

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class MessageConfig {
  // declare queues
  @Bean(name = "indexingQueue")
  public Queue indexingQueue() {
    return QueueBuilder.durable(Messaging.INDEXING_QUEUE).build();
  }

  @Bean(name = "reindexingQueue")
  public Queue reindexingQueue() {
    return QueueBuilder.durable(Messaging.REINDEXING_QUEUE).build();
  }

  @Bean(name = "uploaderQueue")
  public Queue uploaderQueue() {
    return QueueBuilder.durable(Messaging.UPLOAD_QUEUE).build();
  }

  // declare exchanges
  @Bean(name = "indexingExchange")
  public Exchange indexingExchange() {
    return ExchangeBuilder.directExchange(Messaging.INDEXING_EXCHANGE).durable(true).build();
  }

  @Bean(name = "reindexingExchange")
  public Exchange reindexingExchange() {
    return ExchangeBuilder.directExchange(Messaging.REINDEXING_EXCHANGE).durable(true).build();
  }

  @Bean(name = "uploadExchange")
  public Exchange uploadExchange() {
    return ExchangeBuilder.fanoutExchange(Messaging.UPLOAD_EXCHANGE).durable(true).build();
  }

  // bind queues to exchanges
  @Bean(name = "indexBinding")
  public Binding indexBinding() {
    return BindingBuilder.bind(indexingQueue())
        .to(indexingExchange())
        .with(Messaging.INDEXING_QUEUE)
        .noargs();
  }

  @Bean(name = "reindexingBinding")
  public Binding reindexBinding() {
    return BindingBuilder.bind(reindexingQueue())
        .to(reindexingExchange())
        .with(Messaging.REINDEXING_QUEUE)
        .noargs();
  }

  @Bean(name = "uploaderBindings")
  public Binding uploadBinding() {
    return BindingBuilder.bind(uploaderQueue())
        .to(uploadExchange())
        .with(Messaging.UPLOAD_QUEUE)
        .noargs();
  }

  // enable messaging in json
  // note that this class is not the same as the http MessageConverter class
  @Bean
  public MessageConverter getJackson2MessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
