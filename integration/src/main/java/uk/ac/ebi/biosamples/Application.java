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

// import org.openqa.grid.internal.utils.configuration.StandaloneConfiguration;
// import org.openqa.selenium.remote.server.SeleniumServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Application {
  private static Logger log = LoggerFactory.getLogger(Application.class);

  @Bean
  public static PropertySourcesPlaceholderConfigurer getPropertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  public static void exitApplication(final ConfigurableApplicationContext ctx) {
    int exitCode = SpringApplication.exit(ctx, () -> 0);
    log.info("Exit Spring Boot");

    System.exit(exitCode);
  }

  public static void main(String[] args) {
    final ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
    exitApplication(ctx);
  }
}
