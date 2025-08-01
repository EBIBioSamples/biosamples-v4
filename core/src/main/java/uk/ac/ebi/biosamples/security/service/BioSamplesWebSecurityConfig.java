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
package uk.ac.ebi.biosamples.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Component
@Order(99)
public class BioSamplesWebSecurityConfig extends WebSecurityConfigurerAdapter {
  private final BioSamplesTokenAuthenticationService tokenAuthenticationService;

  public BioSamplesWebSecurityConfig(
      final BioSamplesTokenAuthenticationService tokenAuthenticationService) {
    this.tokenAuthenticationService = tokenAuthenticationService;
  }

  private BioSamplesTokenAuthenticationFilter tokenAuthenticationFilter() {
    return new BioSamplesTokenAuthenticationFilter(this.tokenAuthenticationService);
  }

  @Override
  protected void configure(final HttpSecurity httpSecurity) throws Exception {
    httpSecurity
        .csrf()
        .disable()
        .cors()
        .and()
        .exceptionHandling()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeRequests()
        // Allow unrestricted access to /samples/bulk-validate
        .antMatchers(HttpMethod.POST, "/samples/bulk-validate")
        .permitAll()
        .antMatchers(HttpMethod.POST, "/samples/validate")
        .permitAll()
        // Authenticate all other POST requests under /samples
        .antMatchers(HttpMethod.POST, "/samples/**")
        .authenticated()
        // Allow unrestricted access to /actuator endpoints
        .antMatchers("/actuator/**")
        .permitAll()
        // Allow access to all other routes without authentication
        .anyRequest()
        .permitAll();

    httpSecurity.addFilterBefore(
        tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

    // Disable cache control header injection
    httpSecurity.headers().cacheControl().disable();
  }

  // CORS support
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    final CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowCredentials(true);
    configuration.addAllowedOrigin("*");
    configuration.addAllowedHeader("*");
    configuration.addAllowedMethod("*");

    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Autowired
  public void configureGlobal(final AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService());
  }
}
