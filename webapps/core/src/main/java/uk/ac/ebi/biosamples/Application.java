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

import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.spring.autoconfigure.metrics.GcpStackdriverMetricsAutoConfiguration;
import com.google.cloud.spring.core.DefaultGcpProjectIdProvider;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.Executor;
import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.UrlTemplateResolver;
import uk.ac.ebi.biosamples.mongo.repository.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;

@SpringBootApplication(exclude = {GcpStackdriverMetricsAutoConfiguration.class})
@EnableAsync
@EnableCaching
public class Application extends SpringBootServletInitializer {
  public static void main(final String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Value("${spring.cloud.gcp.project-id:no_project}")
  private String enaGcpProject;

  @Autowired private Environment environment;

  @Bean(name = "defaultServiceAccountCredentialFilePath")
  public String getGcpProjectCredentialsFilePath() {
    return environment.getProperty("GOOGLE_APPLICATION_CREDENTIALS");
  }

  @Bean(name = "enaGcpProject")
  public GcpProjectIdProvider enaGcpProject() {
    return new DefaultGcpProjectIdProvider() {
      @Override
      public String getProjectId() {
        return enaGcpProject;
      }
    };
  }

  @Bean(name = "gcpDefaultProjectCredentialsProvider")
  @Primary
  public CredentialsProvider gcpDefaultProjectCredentialsProvider() {
    return () ->
        GoogleCredentials.fromStream(
            Files.newInputStream(Paths.get(getGcpProjectCredentialsFilePath())));
  }

  @Bean
  public StackdriverConfig stackdriverConfig() {
    return new StackdriverConfig() {
      @Override
      public String projectId() {
        return enaGcpProject;
      }

      @Override
      public String get(final String key) {
        return null;
      }

      @Override
      public CredentialsProvider credentials() {
        return gcpDefaultProjectCredentialsProvider();
      }

      @Override
      public boolean useSemanticMetricTypes() {
        return true;
      }

      @Override
      public Duration step() {
        return Duration.ofMinutes(5);
      }
    };
  }

  @Bean
  public StackdriverMeterRegistry meterRegistry(final StackdriverConfig stackdriverConfig) {
    return StackdriverMeterRegistry.builder(stackdriverConfig).build();
  }

  @Bean
  public Filter filter() {
    return new ShallowEtagHeaderFilter();
  }

  /*@Bean
  public HttpMessageConverter<Sample> getXmlSampleHttpMessageConverter(
      final SampleToXmlConverter sampleToXmlConverter) {
    return new SampleAsXMLHttpMessageConverter(sampleToXmlConverter);
  }*/

  @Bean(name = "threadPoolTaskExecutor")
  public Executor threadPoolTaskExecutor() {
    final ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
    ex.setMaxPoolSize(128);
    ex.setQueueCapacity(2056);
    return ex;
  }

  @Bean
  public RepositoryDetectionStrategy repositoryDetectionStrategy() {
    return RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED;
  }

  /* Necessary to render XML using Jaxb2 annotations */
  @Bean
  public Jaxb2RootElementHttpMessageConverter jaxb2RootElementHttpMessageConverter() {
    return new Jaxb2RootElementHttpMessageConverter();
  }

  @Bean
  public CaffeineSpec CaffeineSpec() {
    return CaffeineSpec.parse("maximumSize=500,expireAfterWrite=60s");
  }

  @Bean
  public ITemplateResolver templateResolver() {
    return new UrlTemplateResolver();
  }

  @Bean(name = "SampleAccessionService")
  public MongoAccessionService mongoSampleAccessionService(
      final MongoSampleRepository mongoSampleRepository,
      final SampleToMongoSampleConverter sampleToMongoSampleConverter,
      final MongoSampleToSampleConverter mongoSampleToSampleConverter,
      final MongoOperations mongoOperations) {
    return new MongoAccessionService(
        mongoSampleRepository,
        sampleToMongoSampleConverter,
        mongoSampleToSampleConverter,
        mongoOperations);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
