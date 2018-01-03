package uk.ac.ebi.biosamples.client;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.ac.ebi.biosamples.BioSamplesProperties;

@Configuration
@ConditionalOnClass(HealthIndicator.class)
public class BioSamplesHealthIndicatorConfiguration {

	@Bean
	//@ConditionalOnClass(HealthIndicator.class)
	public BioSamplesHealthIndicator bioSamplesHealthIndicator(RestTemplateBuilder restTemplateBuilder, BioSamplesProperties bioSamplesProperties) {
		return new BioSamplesHealthIndicator(restTemplateBuilder, bioSamplesProperties);
	}
}
