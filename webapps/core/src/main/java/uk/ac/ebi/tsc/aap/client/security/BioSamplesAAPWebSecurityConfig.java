package uk.ac.ebi.tsc.aap.client.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.stereotype.Component;

import uk.ac.ebi.tsc.aap.client.security.AAPWebSecurityAutoConfiguration.AAPWebSecurityConfig;

@Component
@Order(99)
public class BioSamplesAAPWebSecurityConfig extends AAPWebSecurityConfig {
    private Logger log = LoggerFactory.getLogger(AAPWebSecurityConfig.class);

    private final StatelessAuthenticationEntryPoint unauthorizedHandler;
    
    public BioSamplesAAPWebSecurityConfig(StatelessAuthenticationEntryPoint unauthorizedHandler) {
    	this.unauthorizedHandler = unauthorizedHandler;
    }
    
	@Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // we don't need CSRF because our token is invulnerable
                .csrf().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                // don't create session
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // disable page caching
        //httpSecurity.headers().cacheControl();
    }
}
