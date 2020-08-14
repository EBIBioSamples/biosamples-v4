/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.MongoProperties;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.service.XmlAsSampleHttpMessageConverter;
import uk.ac.ebi.biosamples.service.XmlGroupToSampleConverter;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;

// import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@Configuration
public class Application extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public HttpMessageConverter<Sample> getXmlSampleHttpMessageConverter(
      XmlSampleToSampleConverter xmlSampleToSampleConverter,
      XmlGroupToSampleConverter xmlGroupToSampleConverter) {
    return new XmlAsSampleHttpMessageConverter(
        xmlSampleToSampleConverter, xmlGroupToSampleConverter);
  }

  @Bean
  public MongoAccessionService mongoGroupAccessionService(
      MongoSampleRepository mongoSampleRepository,
      SampleToMongoSampleConverter sampleToMongoSampleConverter,
      MongoSampleToSampleConverter mongoSampleToSampleConverter,
      MongoProperties mongoProperties) {
    return new MongoAccessionService(
        mongoSampleRepository,
        sampleToMongoSampleConverter,
        mongoSampleToSampleConverter,
        "SAMEG",
        mongoProperties.getAccessionMinimum(),
        mongoProperties.getAcessionQueueSize());
  }
}
