package uk.ac.ebi.biosamples.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.biosamples.service.AttributeValidator;
import uk.ac.ebi.biosamples.service.SampleValidator;

@Configuration
@ConditionalOnMissingBean(BioSamplesClient.class)
public class BioSamplesAutoConfiguration {

	@Bean	
	public AttributeValidator attributeValidator() {
		return new AttributeValidator();
	}
	
	@Bean	
	public SampleValidator sampleValidator(AttributeValidator attributeValidator) {
		return new SampleValidator(attributeValidator);
	}
	
	@Bean
	public BioSamplesClient bioSamplesClient(ClientProperties clientProperties, 
			RestTemplateBuilder restTemplateBuilder, SampleValidator sampleValidator) {		
		//make sure there is a application/hal+json converter		
		//traverson will make its own but not if we want to customize the resttemplate in any way (e.g. caching)
		restTemplateBuilder = restTemplateBuilder.additionalCustomizers(new RestTemplateCustomizer() {
			public void customize(RestTemplate restTemplate) {
				List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
				
				ObjectMapper mapper = new ObjectMapper();
				mapper.registerModule(new Jackson2HalModule());
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				//MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
				MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
				halConverter.setObjectMapper(mapper);
				halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));

				converters.add(0,halConverter);
				
				restTemplate.setMessageConverters(converters);
			}			
		});
		return new BioSamplesClient(clientProperties, restTemplateBuilder, sampleValidator);
	}
}
