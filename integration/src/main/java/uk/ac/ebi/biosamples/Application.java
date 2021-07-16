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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@SpringBootApplication
// @Configuration
// @EnableAutoConfiguration
// @ComponentScan(lazyInit = true, excludeFilters={
//		  @ComponentScan.Filter(type= FilterType.ASSIGNABLE_TYPE, value=BioSamplesClient.class)})
public class Application {

  // this is needed to read nonstrings from properties files
  // must be static for lifecycle reasons
  @Bean
  public static PropertySourcesPlaceholderConfigurer getPropertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  //	@Bean
  //    public SeleniumServer getSeleniumServer() {
  //        return new SeleniumServer(new StandaloneConfiguration());
  //    }

  public static void main(String[] args) {
    SpringApplication.exit(SpringApplication.run(Application.class, args));
  }
}
