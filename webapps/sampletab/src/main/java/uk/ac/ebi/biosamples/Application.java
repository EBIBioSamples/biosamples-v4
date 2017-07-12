package uk.ac.ebi.biosamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

@SpringBootApplication
@ComponentScan(lazyInit = true, excludeFilters={
		  @ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE, value=BioSamplesClient.class)})
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
/*
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
*/
}
