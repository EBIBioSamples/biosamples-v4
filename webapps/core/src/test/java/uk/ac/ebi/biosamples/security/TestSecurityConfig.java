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
package uk.ac.ebi.biosamples.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@TestConfiguration
public class TestSecurityConfig {
  @Bean
  @Primary
  public UserDetailsService userDetailsService() {
    return new InMemoryUserDetailsManager(
        Arrays.asList(
            new User("Webin-12345", "password", authorities("ENA")),
            new User("Webin-57176", "password", authorities("ENA", "EGA"))));
  }

  private static Collection<SimpleGrantedAuthority> authorities(String... role) {
    return Arrays.stream(role).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
  }
}
