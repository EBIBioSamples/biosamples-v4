package uk.ac.ebi.biosamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.handler.MappedInterceptor;

import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.xml.XmlSampleHttpMessageConverter;

@SpringBootApplication
//@EnableHypermediaSupport(type = { HypermediaType.HAL })
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Bean
	public MappedInterceptor getCacheHeaderMappedInterceptor() {
	    return new MappedInterceptor(new String[]{"/**"}, new CacheControlInterceptor());
	}
	
	@Bean
	public HttpMessageConverter<Sample> getXmlSampleHttpMessageConverter() {
		return new XmlSampleHttpMessageConverter();
	}
	
	//TODO cors
	
}
