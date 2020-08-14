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
package uk.ac.ebi.biosamples.legacy.json;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacyContactMixin;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacyOrganizationMixin;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.Organization;

@Configuration
public class LegacyJsonConfig {

  @Bean
  public Jackson2ObjectMapperBuilder jacksonBuilder() {
    Jackson2ObjectMapperBuilder b = new Jackson2ObjectMapperBuilder();
    b.indentOutput(true)
        .mixIn(Contact.class, LegacyContactMixin.class)
        .mixIn(Organization.class, LegacyOrganizationMixin.class);
    return b;
  }
}
