
package uk.ac.ebi.biosamples.client;



import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.service.AttributeValidator;
import uk.ac.ebi.biosamples.service.SampleValidator;

@Configuration
@AutoConfigureAfter(WebClientAutoConfiguration.class)
public class BioSamplesAutoConfiguration {

   @Bean
   @ConditionalOnMissingBean(AttributeValidator.class)
   public AttributeValidator attributeValidator() {
      return BioSamplesClientBuilder.attributeValidator();
   }

   @Bean
   @ConditionalOnMissingBean(SampleValidator.class)
   public SampleValidator sampleValidator(AttributeValidator attributeValidator) {
      return BioSamplesClientBuilder.sampleValidator(attributeValidator);
   }

   @Bean
   @ConditionalOnMissingBean(BioSamplesProperties.class)
   public BioSamplesProperties bioSamplesProperties() {
      return BioSamplesClientBuilder.bioSamplesProperties();
   }

   @Bean
   @ConditionalOnMissingBean(AapClientService.class)
   public AapClientService aapClientService(RestTemplateBuilder restTemplateBuilder, BioSamplesProperties bioSamplesProperties) {
      return BioSamplesClientBuilder.aapClientService( restTemplateBuilder,  bioSamplesProperties);
   }

   @Bean
   @ConditionalOnMissingBean(BioSamplesClient.class)
   public BioSamplesClient bioSamplesClient(BioSamplesProperties bioSamplesProperties,
         RestTemplateBuilder restTemplateBuilder, SampleValidator sampleValidator, AapClientService aapClientService) {
      return BioSamplesClientBuilder.getInstance(bioSamplesProperties , restTemplateBuilder , sampleValidator , aapClientService );
   }
}
