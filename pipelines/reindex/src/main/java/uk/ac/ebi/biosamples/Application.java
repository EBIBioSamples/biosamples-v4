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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import uk.ac.ebi.biosamples.configuration.ExclusionConfiguration;
import uk.ac.ebi.biosamples.service.EnaConfig;
import uk.ac.ebi.biosamples.service.EnaSampleToBioSampleConversionService;
import uk.ac.ebi.biosamples.service.EraProDao;
import uk.ac.ebi.biosamples.utils.PipelineUtils;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@ComponentScan(
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          value = {EnaConfig.class, EraProDao.class, EnaSampleToBioSampleConversionService.class})
    })
@Import(ExclusionConfiguration.class)
public class Application {

  public static void main(final String[] args) {
    final ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
    PipelineUtils.exitPipeline(ctx);
  }
}
