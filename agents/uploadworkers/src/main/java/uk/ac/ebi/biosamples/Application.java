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

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import uk.ac.ebi.biosamples.mongo.MongoProperties;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    System.exit(SpringApplication.exit(SpringApplication.run(Application.class, args)));
  }

  @Bean
  public MessageConverter getJackson2MessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean("biosamplesFileUploadSubmissionContainerFactory")
  public SimpleRabbitListenerContainerFactory containerFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConcurrentConsumers(5);
    factory.setMaxConcurrentConsumers(5);
    configurer.configure(factory, connectionFactory);

    return factory;
  }

  @Bean(name = "SampleAccessionService")
  public MongoAccessionService mongoSampleAccessionService(
      MongoSampleRepository mongoSampleRepository,
      SampleToMongoSampleConverter sampleToMongoSampleConverter,
      MongoSampleToSampleConverter mongoSampleToSampleConverter,
      MongoProperties mongoProperties) {
    return new MongoAccessionService(
        mongoSampleRepository,
        sampleToMongoSampleConverter,
        mongoSampleToSampleConverter,
        mongoProperties.getAccessionPrefix(),
        mongoProperties.getAccessionMinimum(),
        mongoProperties.getAcessionQueueSize());
  }
}
