package uk.ac.ebi.biosamples;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import uk.ac.ebi.biosamples.model.CustomXmlError;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		// https://github.com/spring-projects/spring-boot/issues/2745
		// The error page on Tomcat deployed application interfere with the
		// proper error handling provided by the default error handler
		//set register error pagefilter false
		setRegisterErrorPageFilter(false);
		builder.sources(Application.class);
		return builder;
	}

	@Bean(name = "marshallingHttpMessageConverter")
	public MarshallingHttpMessageConverter getMarshallingHttpMessageConverter() {
		MarshallingHttpMessageConverter marshallingHttpMessageConverter = new MarshallingHttpMessageConverter();
		marshallingHttpMessageConverter.setMarshaller(getJaxb2Marshaller());
		marshallingHttpMessageConverter.setUnmarshaller(getJaxb2Marshaller());
		return marshallingHttpMessageConverter;
	}

	@Bean(name = "jaxb2Marshaller")
	public Jaxb2Marshaller getJaxb2Marshaller() {
		Map<String, Object> props = new HashMap<>();
		props.put(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setClassesToBeBound(CustomXmlError.class);
		jaxb2Marshaller.setMarshallerProperties(props);
		return jaxb2Marshaller;
	}
}
