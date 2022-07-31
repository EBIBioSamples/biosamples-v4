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
package uk.ac.ebi.tsc.aap.client.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import uk.ac.ebi.tsc.aap.client.security.AAPWebSecurityAutoConfiguration.AAPWebSecurityConfig;

@Component
@Order(99)
public class BioSamplesAAPWebSecurityConfig extends AAPWebSecurityConfig {
  private final StatelessAuthenticationEntryPoint unauthorizedHandler;
  private BioSamplesTokenAuthenticationService tokenAuthenticationService;

  public BioSamplesAAPWebSecurityConfig(
      StatelessAuthenticationEntryPoint unauthorizedHandler,
      BioSamplesTokenAuthenticationService tokenAuthenticationService) {
    this.unauthorizedHandler = unauthorizedHandler;
    this.tokenAuthenticationService = tokenAuthenticationService;
  }

  private StatelessAuthenticationFilter statelessAuthenticationFilterBean() {
    return new StatelessAuthenticationFilter(this.tokenAuthenticationService);
  }

  @Override
  protected void configure(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
        // we don't need CSRF because our token is invulnerable
        .csrf()
        .disable()
        .cors()
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(unauthorizedHandler)
        .and()
        // don't create session
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    httpSecurity.addFilterBefore(
        statelessAuthenticationFilterBean(), UsernamePasswordAuthenticationFilter.class);

    // disable the no-cache header injectection, we'll manage this ourselves
    httpSecurity.headers().cacheControl().disable();
  }

  // CORS support
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowCredentials(true);
    configuration.addAllowedOrigin("*");
    configuration.addAllowedHeader("*");
    configuration.addAllowedMethod("*");
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService());
  }
}
