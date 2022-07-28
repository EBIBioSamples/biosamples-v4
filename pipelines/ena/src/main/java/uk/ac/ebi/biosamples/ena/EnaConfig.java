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
package uk.ac.ebi.biosamples.ena;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class EnaConfig {
  @Bean("eraDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.hikari")
  public DataSource getEraDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean("eraJdbcTemplate")
  public JdbcTemplate getEraJdbcTemplate(@Qualifier("eraDataSource") DataSource eraDataSource) {
    JdbcTemplate jdbc = new JdbcTemplate(eraDataSource);
    // oracle driver defaults to 10
    jdbc.setFetchSize(1000);
    return jdbc;
  }
}
